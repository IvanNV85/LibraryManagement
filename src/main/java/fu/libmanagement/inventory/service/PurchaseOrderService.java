package fu.libmanagement.inventory.service;

import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.inventory.entity.BookCopy;
import fu.libmanagement.inventory.entity.CopyStatus;
import fu.libmanagement.inventory.repository.BookCopyRepository;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/** Nhập kho dùng trực tiếp các bảng hiện có; khóa phiếu trong suốt transaction nhận hàng. */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private final JdbcTemplate jdbc;
    private final BookRepository books;
    private final BookCopyRepository copies;

    public record Supplier(long id, String name) {}
    public record Order(long id, String supplier, String status, String note, String creator,
                        LocalDateTime createdAt, String approver, LocalDateTime approvedAt, LocalDateTime receivedAt) {}
    public record Item(long id, Long bookId, String isbn, String title, int quantity, BigDecimal unitPrice,
                       int receivedQuantity, String discrepancyNote) {}
    public record Line(String isbn, String title, Integer quantity, BigDecimal unitPrice) {}
    public record Receipt(long itemId, Integer quantity, String note) {}

    @Transactional(readOnly = true)
    public List<Supplier> suppliers() {
        return jdbc.query("select id, name from suppliers order by name", (rs, n) -> new Supplier(rs.getLong(1), rs.getString(2)));
    }

    @Transactional(readOnly = true)
    public List<Order> list() {
        return jdbc.query(orderSql() + " order by po.id desc", (rs, n) -> mapOrder(rs));
    }

    @Transactional(readOnly = true)
    public Order order(long id) {
        return jdbc.query(orderSql() + " where po.id = ?", (rs, n) -> mapOrder(rs), id).stream().findFirst()
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu nhập"));
    }

    @Transactional(readOnly = true)
    public List<Item> items(long id) {
        return jdbc.query("select id, book_id, isbn, title, quantity, unit_price, received_quantity, discrepancy_note from purchase_order_items where purchase_order_id = ? order by id",
                (rs, n) -> new Item(rs.getLong(1), (Long) rs.getObject(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getBigDecimal(6), rs.getInt(7), rs.getString(8)), id);
    }

    private String orderSql() {
        return "select po.id, s.name, po.status, po.note, u.full_name, po.created_at, a.full_name, po.approved_at, po.received_at " +
                "from purchase_orders po join suppliers s on s.id=po.supplier_id join users u on u.id=po.created_by left join users a on a.id=po.approved_by";
    }
    private Order mapOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Order(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                date(rs.getTimestamp(6)), rs.getString(7), date(rs.getTimestamp(8)), date(rs.getTimestamp(9)));
    }
    private LocalDateTime date(Timestamp value) { return value == null ? null : value.toLocalDateTime(); }

    @Transactional
    public long create(long supplierId, String note, List<Line> lines, long userId) {
        if (jdbc.queryForObject("select count(*) from suppliers where id = ?", Integer.class, supplierId) == 0)
            throw new BusinessException("Nhà cung cấp không tồn tại");
        if (note != null && note.length() > 255) throw new BusinessException("Ghi chú tối đa 255 ký tự");
        if (lines == null || lines.isEmpty()) throw new BusinessException("Cần ít nhất một dòng sách");
        for (Line l : lines) {
            if (l.isbn() == null || l.isbn().isBlank() || l.isbn().trim().length() > 20 ||
                    l.title() == null || l.title().isBlank() || l.title().trim().length() > 255 ||
                    l.quantity() == null || l.quantity() <= 0 || l.unitPrice() == null ||
                    l.unitPrice().signum() < 0 || l.unitPrice().scale() > 2 || l.unitPrice().compareTo(new BigDecimal("9999999999.99")) > 0)
                throw new BusinessException("Mỗi dòng cần ISBN, tên sách, số lượng dương và đơn giá hợp lệ");
        }
        Long id = jdbc.queryForObject("insert into purchase_orders (supplier_id,status,note,created_by) output inserted.id values (?, 'PENDING', ?, ?)", Long.class, supplierId, note, userId);
        for (Line l : lines) {
            String isbn = l.isbn().trim();
            List<Long> matches = jdbc.queryForList("select id from books where isbn = ?", Long.class, isbn);
            Long bookId = matches.isEmpty() ? null : matches.get(0);
            jdbc.update("insert into purchase_order_items (purchase_order_id,book_id,isbn,title,quantity,unit_price,received_quantity) values (?,?,?,?,?,?,0)",
                    id, bookId, isbn, l.title().trim(), l.quantity(), l.unitPrice());
        }
        return Objects.requireNonNull(id);
    }

    @Transactional
    public void approve(long id, long userId) {
        int changed = jdbc.update("update purchase_orders set status='APPROVED', approved_by=?, approved_at=? where id=? and status='PENDING' and received_at is null", userId, Timestamp.valueOf(LocalDateTime.now()), id);
        if (changed != 1) throw new BusinessException("Phiếu không tồn tại hoặc không còn chờ duyệt");
    }

    @Transactional
    public void receive(long id, List<Receipt> receipts) {
        List<String> states = jdbc.query("select status from purchase_orders with (UPDLOCK, ROWLOCK) where id=? and received_at is null", (rs, n) -> rs.getString(1), id);
        if (states.isEmpty() || !"APPROVED".equals(states.get(0)))
            throw new BusinessException("Phiếu chưa được duyệt hoặc đã nhận kho");
        List<Item> items = items(id);
        if (items.isEmpty() || receipts == null || receipts.size() != items.size()) throw new BusinessException("Dữ liệu nhận hàng không đầy đủ");
        Map<Long, Receipt> byId = new HashMap<>();
        for (Receipt r : receipts) if (byId.put(r.itemId(), r) != null) throw new BusinessException("Dòng nhận hàng bị lặp");
        Map<Long, Book> resolved = new HashMap<>();
        for (Item item : items) {
            Receipt r = byId.get(item.id());
            if (r == null || r.quantity() == null || r.quantity() < 0) throw new BusinessException("Số lượng thực nhận phải không âm");
            if (r.note() != null && r.note().length() > 255) throw new BusinessException("Ghi chú chênh lệch tối đa 255 ký tự");
            if (r.quantity() != item.quantity() && (r.note() == null || r.note().isBlank())) throw new BusinessException("Vui lòng ghi lý do chênh lệch cho " + item.title());
            if (r.quantity() > 0) {
                List<Long> ids = jdbc.queryForList("select id from books where isbn = ?", Long.class, item.isbn());
                if (ids.isEmpty()) throw new BusinessException("Chưa có đầu sách ISBN " + item.isbn() + ". Cần biên mục trước khi nhận kho.");
                Book book = books.findById(ids.get(0)).orElseThrow(() -> new BusinessException("Đầu sách không tồn tại"));
                resolved.put(item.id(), book);
            }
        }
        for (Item item : items) {
            Receipt r = byId.get(item.id());
            Book book = resolved.get(item.id());
            jdbc.update("update purchase_order_items set book_id=coalesce(?,book_id), received_quantity=?, discrepancy_note=? where id=? and purchase_order_id=?",
                    book == null ? null : book.getId(), r.quantity(), r.note(), item.id(), id);
            for (int i = 0; i < r.quantity(); i++) {
                BookCopy copy = new BookCopy();
                copy.setBook(book);
                copy.setBarcode("P" + UUID.randomUUID().toString().replace("-", "").substring(0, 29).toUpperCase(Locale.ROOT));
                copy.setStatus(CopyStatus.AVAILABLE);
                copy.setPrice(item.unitPrice());
                copy.setPurchaseOrderItemId(item.id());
                copies.save(copy);
            }
        }
        copies.flush();
        jdbc.update("update purchase_orders set received_at=? where id=?", Timestamp.valueOf(LocalDateTime.now()), id);
    }
}
