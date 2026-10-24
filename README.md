# Library Management System

Web quản lý thư viện trực tuyến, bài tập nhóm môn HSF302 (Nhóm 2).

Hệ thống hỗ trợ độc giả tìm kiếm, mượn, đặt trước và gia hạn sách; thủ thư xử lý mượn trả, phạt và quản lý đầu sách; quản trị viên quản lý kho, nhân viên, quy định và báo cáo.

Cấu hình (database, ...) nằm trong `src/main/resources/application.properties`. Mỗi người nên chỉnh theo máy của mình.

## Cấu trúc dự án

Tổ chức theo module (modular feature based architecture). Mỗi module có các package `controller`, `service`, `repository`, `entity`.

```
src/main/java/fu/librarymanagement/
├── auth           Đăng ký, đăng nhập, quên mật khẩu
├── user           Hồ sơ, độc giả, nhân viên, phân quyền
├── catalog        Đầu sách, tìm kiếm, lọc, thể loại, tác giả
├── inventory      Bản sao sách, nhập kho
├── borrowing      Yêu cầu mượn, lập phiếu, trả sách, gia hạn, lịch sử
├── reservation    Đặt trước sách đang hết
├── fine           Tiền phạt và thanh toán
├── engagement     Đánh giá, bình luận, yêu thích
├── config         Cấu hình 
└── common         Enum trạng thái, exception, tiện ích dùng chung
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
- Tên của nhánh feature theo format feature/ten-feature
- commit convention:
  + feature:	Làm use case hoặc chức năng mới
  + fix: 	Sửa lỗi	
  + refactor:	Sắp xếp lại code, không đổi hành vi	
  + docs:	Tài liệu, README	
  + chore:	Cấu hình, thư viện, dọn dẹp
- Ví dụ:
  feat: add borrow request API
  fix: lock account after 5 failed logins
  refactor: extract fine calculation to service
  docs: update README with module structure
  chore: add spring-boot-starter-mail
