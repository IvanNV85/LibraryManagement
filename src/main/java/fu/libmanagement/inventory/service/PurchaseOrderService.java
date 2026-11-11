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

    /** Nhà cung cấp dùng trong danh sách lựa chọn khi lập phiếu. */
    public record Supplier(long id, String name) {}
    /** Thông tin phiếu dùng cho danh sách/chi tiết; receivedAt xác định phiếu đã nhận kho hay chưa. */
    public record Order(long id, String supplier, String status, String note, String creator,
                        LocalDateTime createdAt, String approver, LocalDateTime approvedAt, LocalDateTime receivedAt) {}
    /** Dòng phiếu đã lưu, gồm số lượng đặt, số lượng thực nhận và ghi chú chênh lệch. */
    public record Item(long id, Long bookId, String isbn, String title, int quantity, BigDecimal unitPrice,
                       int receivedQuantity, String discrepancyNote) {}
    /** Dữ liệu một dòng sách thủ thư gửi khi lập phiếu. */
    public record Line(String isbn, String title, Integer quantity, BigDecimal unitPrice) {}
    /** Dữ liệu thực nhận của một dòng phiếu, liên kết bằng itemId. */
    public record Receipt(long itemId, Integer quantity, String note) {}

    /** Lấy nhà cung cấp theo tên để điền danh sách lựa chọn trên form tạo phiếu. */
    @Transactional(readOnly = true)
    public List<Supplier> suppliers() {
        return jdbc.query("select id, name from suppliers order by name", (rs, n) -> new Supplier(rs.getLong(1), rs.getString(2)));
    }

    /** Lấy toàn bộ phiếu cho trang danh sách của Admin/thủ thư, phiếu có ID lớn hơn hiển thị trước. */
    @Transactional(readOnly = true)
    public List<Order> list() {
        return jdbc.query(orderSql() + " order by po.id desc", (rs, n) -> mapOrder(rs));
    }

    /** Lấy thông tin một phiếu kèm nhà cung cấp, người lập/người duyệt; báo lỗi nếu không tìm thấy. */
    @Transactional(readOnly = true)
    public Order order(long id) {
        return jdbc.query(orderSql() + " where po.id = ?", (rs, n) -> mapOrder(rs), id).stream().findFirst()
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu nhập"));
    }

    /** Lấy các dòng sách thuộc phiếu, gồm số lượng đặt và kết quả nhận kho, sắp xếp theo ID dòng. */
    @Transactional(readOnly = true)
    public List<Item> items(long id) {
        return jdbc.query("select id, book_id, isbn, title, quantity, unit_price, received_quantity, discrepancy_note from purchase_order_items where purchase_order_id = ? order by id",
                (rs, n) -> new Item(rs.getLong(1), (Long) rs.getObject(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getBigDecimal(6), rs.getInt(7), rs.getString(8)), id);
    }

    /** SQL dùng chung cho danh sách/chi tiết; LEFT JOIN người duyệt để vẫn lấy được phiếu chưa duyệt. */
    private String orderSql() {
        return "select po.id, s.name, po.status, po.note, u.full_name, po.created_at, a.full_name, po.approved_at, po.received_at " +
                "from purchase_orders po join suppliers s on s.id=po.supplier_id join users u on u.id=po.created_by left join users a on a.id=po.approved_by";
    }
    /** Ánh xạ một dòng kết quả SQL sang Order theo đúng thứ tự các cột trong orderSql(). */
    private Order mapOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Order(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                date(rs.getTimestamp(6)), rs.getString(7), date(rs.getTimestamp(8)), date(rs.getTimestamp(9)));
    }
    /** Chuyển thời điểm SQL sang LocalDateTime; giữ null cho ngày duyệt/nhận kho chưa phát sinh. */
    private LocalDateTime date(Timestamp value) { return value == null ? null : value.toLocalDateTime(); }

    /**
     * Kiểm tra nhà cung cấp và các dòng sách, lưu phiếu PENDING cùng chi tiết trong một transaction.
     * ISBN đã có được liên kết với đầu sách; ISBN mới giữ book_id = null để biên mục trước khi nhận kho.
     */
    @Transactional
    public long create(long supplierId, String note, List<Line> lines, long userId) {
        if (jdbc.queryForObject("select count(*) from suppliers where id = ?", Integer.class, supplierId) == 0)
            throw new BusinessException("Nhà cung cấp không tồn tại");
        if (note != null && note.length() > 255) throw new BusinessException("Ghi chú tối đa 255 ký tự");
        if (lines == null || lines.isEmpty()) throw new BusinessException("Cần ít nhất một dòng sách");
        // Số lượng phải dương; đơn giá không âm và phù hợp cột DECIMAL(12,2) trong database.
        for (Line l : lines) {
            if (l.isbn() == null || l.isbn().isBlank() || l.isbn().trim().length() > 20 ||
                    l.title() == null || l.title().isBlank() || l.title().trim().length() > 255 ||
                    l.quantity() == null || l.quantity() <= 0 || l.unitPrice() == null ||
                    l.unitPrice().signum() < 0 || l.unitPrice().scale() > 2 || l.unitPrice().compareTo(new BigDecimal("9999999999.99")) > 0)
                throw new BusinessException("Mỗi dòng cần ISBN, tên sách, số lượng dương và đơn giá hợp lệ");
        }
        // OUTPUT inserted.id lấy ID phiếu vừa tạo để liên kết các dòng chi tiết; chưa tạo bản sao ở bước này.
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

    /** Admin duyệt phiếu PENDING chưa nhận kho, chuyển sang APPROVED và lưu người duyệt/thời điểm duyệt. */
    @Transactional
    public void approve(long id, long userId) {
        // Kiểm tra trạng thái ngay trong UPDATE để không duyệt lại phiếu khi có các request đồng thời.
        int changed = jdbc.update("update purchase_orders set status='APPROVED', approved_by=?, approved_at=? where id=? and status='PENDING' and received_at is null", userId, Timestamp.valueOf(LocalDateTime.now()), id);
        if (changed != 1) throw new BusinessException("Phiếu không tồn tại hoặc không còn chờ duyệt");
    }

    /**
     * Nhận kho một lần cho phiếu APPROVED: kiểm tra toàn bộ dòng, lưu thực nhận và tạo bản sao AVAILABLE.
     * Các cập nhật phiếu/dòng phiếu và bản sao nằm trong cùng transaction; lỗi sẽ rollback toàn bộ.
     */
    @Transactional
    public void receive(long id, List<Receipt> receipts) {
        // Giữ khóa cập nhật trên phiếu đến hết transaction để tránh hai request cùng nhận kho một phiếu.
        List<String> states = jdbc.query("select status from purchase_orders with (UPDLOCK, ROWLOCK) where id=? and received_at is null", (rs, n) -> rs.getString(1), id);
        if (states.isEmpty() || !"APPROVED".equals(states.get(0)))
            throw new BusinessException("Phiếu chưa được duyệt hoặc đã nhận kho");
        List<Item> items = items(id);
        if (items.isEmpty() || receipts == null || receipts.size() != items.size()) throw new BusinessException("Dữ liệu nhận hàng không đầy đủ");
        // Ghép dữ liệu theo ID dòng phiếu, từ chối ID lặp và kiểm tra đủ các dòng trước khi ghi nhận kho.
        Map<Long, Receipt> byId = new HashMap<>();
        for (Receipt r : receipts) if (byId.put(r.itemId(), r) != null) throw new BusinessException("Dòng nhận hàng bị lặp");
        // Kiểm tra toàn bộ số lượng/ghi chú và tìm đầu sách theo ISBN trước khi tạo bất kỳ bản sao nào.
        Map<Long, Book> resolved = new HashMap<>();
        for (Item item : items) {
            Receipt r = byId.get(item.id());
            if (r == null || r.quantity() == null || r.quantity() < 0) throw new BusinessException("Số lượng thực nhận phải không âm");
            if (r.note() != null && r.note().length() > 255) throw new BusinessException("Ghi chú chênh lệch tối đa 255 ký tự");
            // Thực nhận có thể khác số lượng đặt, nhưng bắt buộc có lý do khi chênh lệch.
            if (r.quantity() != item.quantity() && (r.note() == null || r.note().isBlank())) throw new BusinessException("Vui lòng ghi lý do chênh lệch cho " + item.title());
            if (r.quantity() > 0) {
                // Chỉ dòng thực nhận lớn hơn 0 cần đầu sách đã biên mục; không tự tạo đầu sách tại đây.
                List<Long> ids = jdbc.queryForList("select id from books where isbn = ?", Long.class, item.isbn());
                if (ids.isEmpty()) throw new BusinessException("Chưa có đầu sách ISBN " + item.isbn() + ". Cần biên mục trước khi nhận kho.");
                Book book = books.findById(ids.get(0)).orElseThrow(() -> new BusinessException("Đầu sách không tồn tại"));
                resolved.put(item.id(), book);
            }
        }
        // Sau khi mọi dòng hợp lệ, lưu kết quả thực nhận và tạo một BookCopy cho mỗi cuốn thực nhận.
        for (Item item : items) {
            Receipt r = byId.get(item.id());
            Book book = resolved.get(item.id());
            jdbc.update("update purchase_order_items set book_id=coalesce(?,book_id), received_quantity=?, discrepancy_note=? where id=? and purchase_order_id=?",
                    book == null ? null : book.getId(), r.quantity(), r.note(), item.id(), id);
            for (int i = 0; i < r.quantity(); i++) {
                BookCopy copy = new BookCopy();
                copy.setBook(book);
                // Barcode dài 30 ký tự; giữ đơn giá và dòng phiếu nguồn để truy vết bản sao nhập kho.
                copy.setBarcode("P" + UUID.randomUUID().toString().replace("-", "").substring(0, 29).toUpperCase(Locale.ROOT));
                copy.setStatus(CopyStatus.AVAILABLE);
                copy.setPrice(item.unitPrice());
                copy.setPurchaseOrderItemId(item.id());
                copies.save(copy);
            }
        }
        // Flush bản sao trước khi đánh dấu nhận xong; giữ status APPROVED, dùng received_at để chặn nhận lại.
        copies.flush();
        jdbc.update("update purchase_orders set received_at=? where id=?", Timestamp.valueOf(LocalDateTime.now()), id);
    }
}
