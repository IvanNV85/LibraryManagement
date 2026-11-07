# Master Project Rules - Library Management System (HSF302 - Nhóm 2)

Tập hợp các quy tắc bắt buộc áp dụng cho toàn bộ dự án `LibraryManagementSystem`. Mọi Agent hoặc lập trình viên khi làm việc trong repository này đều phải tuân thủ nghiêm ngặt các quy tắc dưới đây.

---

## 1. Phân quyền và Quy chuẩn Định tuyến URL (URL Security & Authorization)

Hệ thống phân quyền dựa trên tiền tố URL và đã được chặn tự động bởi `RoleInterceptor` trong `fu.libmanagement.common.config.WebConfig`. Mọi Controller mới tạo phải tuân thủ đúng tiền tố:

| Tiền tố URL | Đối tượng được phép truy cập | Ghi chú |
|---|---|---|
| `/account/**` | Mọi người dùng đã đăng nhập (`READER`, `LIBRARIAN`, `ADMIN`) | Hồ sơ cá nhân, đổi mật khẩu |
| `/reader/**` | Chỉ dành cho Độc giả (`READER`) | Lịch sử mượn, gửi yêu cầu, đặt trước, thông báo |
| `/librarian/**` | Thủ thư và Quản trị viên (`LIBRARIAN`, `ADMIN`) | Duyệt mượn, lập phiếu, trả sách, xử lý phạt, quản lý đầu sách, bản sao |
| `/admin/**` | Dành riêng cho Quản trị viên (`ADMIN`) | Quản lý nhân viên, duyệt phiếu nhập, cấu hình quy định, báo cáo |
| Công khai | Khách (`Guest`) và người dùng vãng lai | `/`, `/books/**`, `/login`, `/register`, `/auth/**` |

* **Lấy thông tin User hiện tại:**
  * Trong Controller: `SessionUser user = SessionUser.get(request);` (hoặc kiểm tra `user.isStaff()`, `user.isReader()`).
  * Trong Thymeleaf: Biến `${currentUser}` luôn có sẵn trên mọi view.

---

## 2. Quy tắc Nghiệp vụ Giản lược (Business Rules theo Phân công)

Dự án tuân theo tiêu chí **đơn giản hóa** để phù hợp với môn học HSF302. Tuyệt đối **KHÔNG** over-engineer các tính năng đã thống nhất cắt giảm:

1. **Xác thực & Bảo mật:**
   * Không tích hợp dịch vụ SMS OTP bên ngoài.
   * Không làm khóa tài khoản tự động phức tạp khi gõ sai mật khẩu nhiều lần (đơn giản hóa theo `Phân công.docx`).
2. **Quy định về Đầu sách & Bản sao (UC20, UC21):**
   * **Quy tắc trùng ISBN:** Khi thêm sách mới, nếu ISBN đã tồn tại trong bảng `books`, hệ thống **chặn tạo mới** và thông báo hướng dẫn người dùng chuyển sang thêm bản sao (`BookCopy`) tại UC21.
   * **Xóa mềm (Soft Delete):** Không thực hiện `DELETE` cứng các bản ghi `Book` hoặc `User` để tránh gãy khóa ngoại. Dùng cờ `visible = false` cho sách và `status = 'LOCKED'` cho tài khoản/thẻ.
3. **Quy định về Mượn - Trả sách (UC11 – UC19):**
   * **Điều kiện mượn:** Độc giả chỉ được mượn khi:
     1. Thẻ thư viện còn hạn (`CardStatus == ACTIVE` và `expiryDate >= TODAY`).
     2. Không có khoản phạt chưa thanh toán (`FineStatus == UNPAID`).
     3. Số sách đang mượn chưa vượt quá giới hạn cho phép.
   * **Đọc quy định từ CSDL:** Các thông số: *Số sách tối đa (`MAX_BOOKS_PER_READER`), số ngày mượn (`LOAN_DAYS`), mức phạt mỗi ngày (`FINE_PER_DAY`), số ngày gia hạn (`RENEW_DAYS`)* **phải được đọc từ bảng `settings`** (UC24), không được hardcode con số cố định trong code nghiệp vụ.
   * **Gia hạn sách:** Chỉ cho gia hạn tối đa 1 lần, cộng thêm 7 ngày và chỉ được gia hạn khi sách chưa quá hạn.
   * **Xử lý phạt:** Đơn giản hóa: Phạt trễ = `số ngày trễ × FINE_PER_DAY`. Thủ thư bấm nút *"Đã thu phạt"* tại quầy (không tích hợp cổng thanh toán trực tuyến VNPay/MoMo).
   * **Đặt trước (Reservation):** Chỉ ghi nhận danh sách độc giả đặt trước; thủ thư xem danh sách này và tự liên hệ (không làm hàng chờ tự động chuyển trạng thái).

---

## 3. Kiến trúc Mã nguồn & Phân tách Module (Modular Architecture)

Dự án được tổ chức theo module tính năng tại `fu.libmanagement.*`:

```
fu.libmanagement
├── auth          (Đăng ký, đăng nhập, bảo mật)
├── user          (Hồ sơ người dùng, thẻ độc giả, tài khoản nhân viên)
├── catalog       (Đầu sách, tìm kiếm, tác giả, thể loại)
├── inventory     (Bản sao sách, phiếu nhập sách, nhà cung cấp)
├── borrowing     (Yêu cầu mượn, phiếu mượn trả, gia hạn)
├── reservation   (Đặt trước sách khi hết bản sao)
├── fine          (Tính phạt và thanh toán phạt)
├── engagement    (Yêu thích, đánh giá, bình luận)
└── common        (Enums dùng chung, Exception, WebConfig, Utility)
```

* **Nguyên tắc độc lập:** Mỗi thành viên phụ trách trọn vẹn luồng `Entity -> Repository -> Service -> Controller -> View` của use case được phân công.
* **Tái sử dụng service:** Khi cần kiểm tra chéo (ví dụ: Mượn sách cần kiểm tra thẻ độc giả), hãy inject Service của module liên quan (hoặc gọi qua Repository công khai), không duplicate logic.
* **Xử lý Enum:** Toàn bộ trạng thái nghiệp vụ phải dùng Enum trong package `fu.libmanagement.common.enums`. Tất cả Enum phải có `@Getter private final String label;` (tiếng Việt).
* **Xử lý Ngoại lệ:** Ném `BusinessException("Lý do lỗi bằng tiếng Việt")`. Controller bắt ngoại lệ và trả lại form hoặc redirect kèm flash attribute `"error"`.

---

## 4. Quy chuẩn Giao diện & Layout Thymeleaf

1. **Phân hệ Độc giả & Công khai (Storefront):** Sử dụng các fragment từ `fragments/layout`:
   * `<head th:replace="~{fragments/layout :: head('Tiêu đề')}"></head>`
   * `<div th:replace="~{fragments/layout :: navbar}"></div>`
   * `<div th:replace="~{fragments/layout :: alerts}"></div>`
   * `<nav th:replace="~{fragments/layout :: pagination(${result}, @{...})}"></nav>`
   * `<div th:replace="~{fragments/layout :: footer}"></div>`
   * Khung nội dung chính: `<main class="container my-4"><div class="panel page-spacer">...</div></main>`.

2. **Phân hệ Thủ thư & Quản trị viên (Dashboard có Sidebar):** Sử dụng `fragments/layout-librarian`:
   * Bao bọc toàn trang bằng: `<div class="dashboard-layout"> ... </div>`
   * Sidebar điều hướng cố định: `<aside th:replace="~{fragments/layout-librarian :: sidebar('activeTabName')}"></aside>`
   * Header thanh làm việc: `<header th:replace="~{fragments/layout-librarian :: topbar('Tiêu đề màn hình')}"></header>`
   * Vùng nội dung chính: `<main class="dashboard-content"><div class="panel border-0 shadow-sm p-4">...</div></main>`
   * Luôn hỗ trợ link nhanh quay lại giao diện độc giả.

3. **Màu sắc & CSS:** Sử dụng CSS variables từ `style.css` (`--lib-primary: #1d6fb8`, `--lib-bg: #f1ede0`, `--lib-border: #ded8c6`). Không hardcode mã màu lạ vào thẻ `style=""`.
4. **Đăng ký Menu:** Bắt buộc phải thêm đường dẫn chức năng vào đúng file menu:
   * Độc giả: `templates/fragments/menu-reader.html`
   * Thủ thư: `templates/fragments/menu-librarian.html`
   * Admin: `templates/fragments/menu-admin.html`
   Cú pháp: `<a class="account-item" th:href="@{...}"><span class="icon-circle"><i class="bi bi-..."></i></span> Tên mục</a>`

---

## 5. Quy chuẩn Git & Cơ sở dữ liệu

* **Nhánh làm việc:** Mỗi use case tạo nhánh riêng `feature/ten-chuc-nang`, tạo Pull Request vào nhánh `develop`.
* **Commit convention:**
  * `feat:` Tính năng mới
  * `fix:` Sửa lỗi
  * `refactor:` Tái cấu trúc code không đổi logic
  * `docs:` Tài liệu, README
  * `chore:` Cấu hình, thư viện, dọn dẹp
* **Database Credentials:** Tuyệt đối **KHÔNG commit** thay đổi username/password database trong `application.properties` (giữ nguyên mặc định `sa` / `sa`).

