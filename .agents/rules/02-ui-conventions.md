# UI & Thymeleaf Coding Conventions

Quy tắc bắt buộc khi tạo view giao diện Thymeleaf:

## 1. Khung trang và Fragments
- Luôn bọc nội dung bằng:
  ```html
  <head th:replace="~{fragments/layout :: head('Tiêu đề')}"></head>
  <div th:replace="~{fragments/layout :: navbar}"></div>
  <main class="container my-4">
      <div class="panel page-spacer">
          <div th:replace="~{fragments/layout :: alerts}"></div>
          <!-- Nội dung -->
      </div>
  </main>
  <div th:replace="~{fragments/layout :: footer}"></div>
  ```
- Dùng `<div th:replace="~{fragments/layout :: alerts}"></div>` để nhận flash attributes `message` (thành công) và `error` (thất bại).

## 2. Bảng và Form
- Bảng quản trị: `<table class="table table-hover align-middle">` với `thead class="table-light"`.
- Nút bấm thao tác trên dòng: dùng `btn-group-sm`.
- Form validation: Thẻ `<input>` dùng `th:field="*{...}"` kèm `th:classappend="${#fields.hasErrors('...')} ? 'is-invalid'"`.
- Dưới mỗi input bị lỗi: `<div class="invalid-feedback" th:errors="*{...}"></div>`.

## 3. Menu phân quyền
- Mọi chức năng mới sau khi tạo phải được đăng ký vào một trong 3 file menu trong `templates/fragments/`:
  - `menu-reader.html`: Dành cho Độc giả.
  - `menu-librarian.html`: Dành cho Thủ thư & Admin.
  - `menu-admin.html`: Dành riêng cho Quản trị viên.

