# Library Management System

Web quản lý thư viện trực tuyến, bài tập nhóm môn HSF302 (Nhóm 2). Bản này đã **đơn giản hóa** theo tài liệu phân công mới (25 UC).

Cấu hình (database, ...) nằm trong `src/main/resources/application.properties`. Mỗi người chỉnh theo máy của mình, nhưng **không commit** thay đổi mật khẩu DB.

## Thành viên

| Thành viên | Use case | Mảng |
|---|---|---|
| An Đặng | UC1 - UC5 | Tài khoản (User), tìm kiếm sách |
| Gia Huy | UC6 - UC10 | Sách phía độc giả (Book, Favorite, Review) |
| Huy Quang | UC11 - UC15 | Mượn trả phía độc giả (BorrowRequest, Loan) |
| Văn An | UC16 - UC20 | Thủ thư xử lý mượn trả và đầu sách |
| Khoa | UC21 - UC25 | Kho sách và quản trị |

Actor: Khách (Guest), Độc giả (Reader), Thủ thư (Librarian), Quản trị viên (Admin).

## Cấu trúc dự án

Tổ chức theo module. Mỗi module có các package `controller`, `service`, `repository`, `entity` (và `dto` nếu cần). Module mới tạo thêm package cùng cấp, **không sửa code của module khác**.

```
src/main/java/fu/libmanagement/
├── auth           UC1 Đăng ký, UC2 Đăng nhập/Đăng xuất, UC3 Đổi mật khẩu
├── user           UC4 Hồ sơ; User, ReaderCard; kiểm tra thẻ (ReaderCardService)
├── catalog        UC5 Tìm kiếm sách; Book, Author, Category
└── common         Enum, exception, cấu hình (config), tiện ích dùng chung
```

## Chạy dự án

1. Chạy file `src/main/resources/db/library_management_full.sql` trong SQL Server (SSMS / Azure Data Studio, hoặc `sqlcmd -I`). Script tạo DB `libraryManagementdb` cùng **toàn bộ bảng của cả nhóm** và dữ liệu mẫu.
   **Cẩn thận:** đầu script có phần RESET xóa hết bảng để dựng lại, đừng chạy trên DB có dữ liệu cần giữ.
2. Sửa `spring.datasource.username/password` trong `application.properties` cho khớp máy bạn.
3. Chạy `LibraryManagementSystemApplication`, mở http://localhost:9999

Hibernate để `hbm2ddl.auto=none`, tức **không tự tạo hay sửa bảng**. Mọi thay đổi DB đều sửa trong file SQL khung chung.

Tài khoản mẫu:

| Email | Mật khẩu | Vai trò |
|---|---|---|
| admin@library.local | Admin@123 | ADMIN |
| librarian@library.local | Librarian@123 | LIBRARIAN |
| reader@library.local | Reader@123 | READER (thẻ đã duyệt) |

## Chức năng UC1 - UC5

| UC | Chức năng | URL | Ai dùng |
|---|---|---|---|
| 1 | Đăng ký (họ tên, email, mật khẩu; chặn email trùng; thẻ ở trạng thái PENDING chờ duyệt) | `/register` | Khách |
| 2 | Đăng nhập, đăng xuất, phân quyền theo role | `/login`, `/logout` | Tất cả |
| 3 | Đổi mật khẩu khi đã đăng nhập | `/account/change-password` | Mọi role đã đăng nhập |
| 4 | Xem và sửa hồ sơ (họ tên, SĐT, địa chỉ); email và mã thẻ chỉ xem | `/reader/profile` | Độc giả |
| 5 | Tìm sách theo tên, tác giả, ISBN, có phân trang và gợi ý từ khóa | `/books` | Khách, độc giả |

Quy tắc: mật khẩu tối thiểu 6 ký tự; SĐT để trống được, nếu nhập phải đúng 10 chữ số bắt đầu bằng 0; tài khoản có `users.status = LOCKED` không đăng nhập được.

Các tính năng của bản cũ **đã bỏ**: xác thực email, quên mật khẩu, khóa tạm khi đăng nhập sai, gửi email, ảnh đại diện, tìm theo thể loại.

## Phần dùng chung cho các UC sau (không cần sửa code của UC1 - UC5)

### Phân quyền theo tiền tố URL

Cấu hình trong `common/config/WebConfig.java`. Khi làm màn hình mới chỉ cần đặt URL đúng tiền tố, quyền được áp dụng tự động:

| Tiền tố | Ai được vào |
|---|---|
| `/account/**` | Mọi người đã đăng nhập |
| `/reader/**` | Độc giả |
| `/librarian/**` | Thủ thư, Admin |
| `/admin/**` | Admin |
| Các URL khác (`/books`, `/login`...) | Công khai |

Chưa đăng nhập thì chuyển về `/login`; sai quyền thì hiện trang 403.

### Lấy người dùng hiện tại

- Controller: `SessionUser user = SessionUser.get(request);` (null nếu chưa đăng nhập; URL có tiền tố ở trên luôn có giá trị). Có `getId()`, `getFullName()`, `getRole()`, `isReader()`, `isLibrarian()`, `isAdmin()`, `isStaff()`.
- Thymeleaf: biến `currentUser` có sẵn trên mọi view.
- Cần entity đầy đủ thì `userService.getUser(user.getId())`.

### Giao diện dùng chung (`templates/fragments/layout.html`)

| Fragment | Dùng để |
|---|---|
| `head(title)`, `navbar`, `footer` | Khung trang. Mỗi trang gọi `head`, `navbar` ở đầu và `footer` ở cuối |
| `alerts` | Hiện `${error}` (đỏ) và `${message}` (xanh). Sau redirect dùng `redirectAttributes.addFlashAttribute("message", "...")` |
| `pagination(page, url)` | Phân trang cho `Page<...>` của Spring. Ví dụ: `<nav th:replace="~{fragments/layout :: pagination(${result}, @{/books(keyword=${keyword})})}"></nav>` |

**Thêm mục vào menu tài khoản:** mỗi vai trò có một file riêng, chỉ cần copy thẻ `<a class="account-item">` vào đó (có hướng dẫn trong file), không sửa `layout.html`:

- Độc giả: `fragments/menu-reader.html`
- Thủ thư (admin cũng thấy): `fragments/menu-librarian.html`
- Admin: `fragments/menu-admin.html`

### Xử lý lỗi

- Service ném `BusinessException("thông báo")` khi vi phạm nghiệp vụ. Controller bắt và hiện `e.getMessage()` để giữ lại form đang nhập.
- Nếu controller không bắt, `GlobalExceptionHandler` hiện trang `error/business` với đúng thông báo đó.
- Trang lỗi 403, 404, 5xx nằm ở `templates/error/`.

### Service và tiện ích dùng lại

| Lớp | Công dụng |
|---|---|
| `ReaderCardService.requireValidCard(userId)` | Kiểm tra thẻ ACTIVE và chưa hết hạn, sai thì ném `BusinessException` nêu lý do. **UC11 và UC17 gọi hàm này** để kiểm tra "thẻ còn hạn". Còn có `isCardValid`, `getCard` (UC6 dùng để duyệt thẻ) |
| `UserService.getUser(id)` | Lấy tài khoản theo id |
| `BookService`, `BookRepository` | Tìm kiếm sách (UC5). UC7, UC8, UC20 thêm phương thức mới vào đây hoặc tạo service riêng |
| `FileStorageService` | Lưu ảnh JPG/PNG tối đa 2MB (ảnh bìa UC20), trả về URL `/uploads/...` |
| `ValidationUtil` | Chuẩn hóa và kiểm tra email, họ tên, SĐT |
| `Role`, `AccountStatus`, `CardStatus` | Enum khớp CHECK constraint trong DB |

### Entity đã có (các UC sau dùng lại, không tạo lại)

- `user`: `User` (bảng `users`), `ReaderCard` (bảng `reader_cards`)
- `catalog`: `Book`, `Author`, `Category`

Các bảng còn lại (`book_copies`, `loans`, `settings`, ...) đã có trong script SQL, mỗi người tự viết entity cho bảng của mình. Cột không dùng trong `users` (`avatar_url`, `email_verified`, `failed_login_count`, `locked_until`) và bảng `auth_tokens` được để nguyên trong DB, code không map.

## Quy ước làm việc

- Mỗi người làm feature nhánh riêng, tạo Pull Request vào `develop`.
- Tên nhánh theo format `feature/ten-feature`.
- Commit convention: `feat` (use case mới), `fix` (sửa lỗi), `refactor` (không đổi hành vi), `docs` (tài liệu), `chore` (cấu hình, thư viện).
  Ví dụ: `feat: add borrow request`, `fix: validate phone on profile`.
