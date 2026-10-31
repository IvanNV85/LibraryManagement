# Library Management System

Web quản lý thư viện trực tuyến, bài tập nhóm môn HSF302 (Nhóm 2).

Hệ thống hỗ trợ độc giả tìm kiếm, mượn, đặt trước và gia hạn sách; thủ thư xử lý mượn trả, phạt và quản lý đầu sách; quản trị viên quản lý kho, nhân viên, quy định và báo cáo.

Cấu hình (database, ...) nằm trong `src/main/resources/application.properties`. Mỗi người nên chỉnh theo máy của mình, nhưng **không commit** thay đổi mật khẩu DB (mặc định trên GitHub là `sa` / `sa`).

## Cấu trúc dự án

Tổ chức theo module (modular feature based architecture). Mỗi module có các package `controller`, `service`, `repository`, `entity`.

```
src/main/java/fu/libmanagement/
├── auth           Đăng ký, đăng nhập, quên mật khẩu
├── user           Hồ sơ, độc giả, nhân viên, phân quyền
├── catalog        Đầu sách, tìm kiếm, lọc, thể loại, tác giả
├── inventory      Bản sao sách, nhập kho
├── borrowing      Yêu cầu mượn, lập phiếu, trả sách, gia hạn, lịch sử
├── reservation    Đặt trước sách đang hết
├── fine           Tiền phạt và thanh toán
├── engagement     Đánh giá, bình luận, yêu thích
└── common         Enum trạng thái, exception, cấu hình (config), tiện ích dùng chung
```

## Vai trò (Actor)

Khách (Guest), Độc giả (Reader), Thủ thư (Librarian), Quản trị viên (Admin).

## Thành viên

| Thành viên | Use case |
|---|---|
| An Đặng | UC1 - UC5 |
| Gia Huy | UC6 - UC10 |
| Huy Quang | UC11 - UC15 |
| Văn An | UC16 - UC20 |
| Đoàn Khoa | UC21 - UC25 |

## Quy ước làm việc

- Mỗi người làm feature nhánh riêng, tạo Pull Request vào `develop`.
- Tên của nhánh feature theo format `feature/ten-feature`
- commit convention:
  + feat: Làm use case hoặc chức năng mới
  + fix: Sửa lỗi
  + refactor: Sắp xếp lại code, không đổi hành vi
  + docs: Tài liệu, README
  + chore: Cấu hình, thư viện, dọn dẹp
- Ví dụ:
  + feat: add borrow request API
  + fix: lock account after 5 failed logins
  + refactor: extract fine calculation to service
  + docs: update README with module structure
  + chore: add spring-boot-starter-mail

## Hướng dẫn chạy và phần dùng chung (UC1 - UC5)

### Cơ sở dữ liệu

Chạy file `src/main/resources/db/01_account_and_catalog.sql` trong SQL Server (tạo DB `libraryManagementdb`, các bảng và dữ liệu mẫu). Script đã có sẵn ảnh bìa cho 12 sách mẫu (ảnh nằm trong `src/main/resources/static/images/covers/`, được commit cùng code).

Nếu máy bạn **đã có DB cũ** (chạy 01 từ trước, chưa có ảnh bìa) thì chạy thêm `db/01b_book_covers_update.sql` một lần. Máy tạo DB mới chỉ cần chạy 01.

Ảnh bìa: đổi ảnh cho từng sách tại `/librarian/books/{id}/cover` (Thủ thư/Admin; ảnh lưu vào `uploads/covers/`, không commit). Muốn ảnh dùng chung cả nhóm thì chép file vào `static/images/covers/` và đặt `cover_url` dạng `/images/covers/ten-file.jpg`. Mỗi người thêm script riêng cho bảng của mình theo dạng `db/02_...sql`, không sửa script của người khác.

Quy tắc mật khẩu: ít nhất 8 ký tự, gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt (ví dụ `Admin@123`). Cấu hình đăng nhập sai, thời hạn token nằm ở `application.properties` (`app.auth.*`).

Tài khoản mẫu:

| Email | Mật khẩu | Vai trò |
|---|---|---|
| admin@library.local | Admin@123 | ADMIN |
| librarian@library.local | Librarian@123 | LIBRARIAN |
| reader@library.local | Reader@123 | READER (thẻ đã duyệt) |

### Gửi email

Đặt biến môi trường `MAIL_USERNAME` và `MAIL_PASSWORD` (Gmail dùng App password). Không ghi mật khẩu vào `application.properties`. Nếu chưa cấu hình, nội dung email (kèm liên kết) được in ra log.

### Phân quyền theo tiền tố URL

Cấu hình trong `common/config/WebConfig.java`. Khi thêm màn hình mới, đặt URL theo tiền tố của vai trò:

| Tiền tố | Ai được vào |
|---|---|
| `/account/**` | Mọi người đã đăng nhập |
| `/reader/**` | Độc giả |
| `/librarian/**` | Thủ thư, Admin |
| `/admin/**` | Admin |
| Các URL khác (`/books`, `/login`...) | Công khai |

### Lấy người dùng hiện tại

- Trong controller: `SessionUser user = SessionUser.get(request);` (null nếu chưa đăng nhập; các URL có tiền tố ở trên luôn có giá trị).
- Trong Thymeleaf: biến `currentUser` (vd `${currentUser.fullName}`), có sẵn trên mọi view.
- Giao diện dùng chung nằm ở `templates/fragments/layout.html` (CSS dùng chung ở `static/css/style.css`, đã nhúng sẵn trong `head`): `head`, `navbar` (header có ô tìm kiếm, nút Đăng nhập/Đăng ký, menu tài khoản mở bằng nút ba gạch), `footer`, `avatar`. Mỗi trang gọi `head(...)`, `navbar` ở đầu và `footer` ở cuối.
- Thêm chức năng vào menu tài khoản: copy một thẻ `<a class="account-item">` trong `layout.html` (có hướng dẫn trong comment). Dùng `th:unless="${currentUser.staff}"` cho chức năng độc giả, `th:if="${currentUser.staff}"` cho thủ thư/admin.
- Ô tìm kiếm trong header là ô tìm kiếm duy nhất của web (UC5, gửi về `/books`). UC7 (lọc, sắp xếp) nên thêm vào trang `catalog/book-list.html`.
- Trang chủ `/` do `HomeController` (module catalog) phụ trách.

### Tiện ích dùng chung (package `common`)

| Lớp | Công dụng |
|---|---|
| `BusinessException` | Service ném khi vi phạm nghiệp vụ, controller bắt và hiện `e.getMessage()` |
| `MailService` | Gửi email bất đồng bộ: `send` (nội dung thường), `sendLink` (email có liên kết kèm token). UC15 dùng lại |
| `FileStorageService` | Lưu ảnh JPG/PNG (ảnh bìa sách UC20 dùng lại), trả về URL `/uploads/...` |
| `ValidationUtil` | Chuẩn hóa email (`normalizeEmail`), kiểm tra email, SĐT, họ tên |
| `Role`, `AccountStatus`, `CardStatus` | Enum vai trò, trạng thái tài khoản, trạng thái thẻ |

### Entity thuộc UC1 - UC5

- `user`: `User` (bảng `users`), `ReaderCard` (thẻ thư viện; UC8 kiểm tra thẻ còn hạn, UC23 duyệt thẻ dùng entity này).
- `auth`: `AuthToken` (token xác thực email, đặt lại mật khẩu).
- `catalog`: `Book`, `Author`, `Category` (bản tối thiểu để tìm kiếm; UC20 bổ sung thêm chức năng trên các entity này, không tạo lại).
