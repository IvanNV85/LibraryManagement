---
name: library-design-system
description: >-
  Design system and implementation guidelines for the Library Management System (HSF302).
  Use this skill whenever creating, styling, or updating UI templates (Thymeleaf, HTML, CSS),
  controllers, forms, tables, badges, dialogs, and menu items to ensure strict visual and
  architectural consistency across all modules and team members.
---

# Library Management System - Design System & UI Guidelines

Bộ quy chuẩn thiết kế giao diện (UI/UX) và kiến trúc màn hình dành cho dự án **Library Management System** (Nhóm 2 – HSF302). Tất cả các thành viên khi tạo use case mới cần tuân thủ triệt để tài liệu này để toàn bộ hệ thống đồng nhất về màu sắc, bố cục, font chữ, hành vi và trải nghiệm người dùng.

---

## 1. Nền tảng Thiết kế (Design Tokens & CSS Variables)

Hệ thống sử dụng **Bootstrap 5.3.3**, **Bootstrap Icons 1.11.3** và tệp định kiểu dùng chung tại `src/main/resources/static/css/style.css`.

### Bảng màu chủ đạo (Color Palette)

| Tên biến CSS | Mã màu | Ý nghĩa / Ứng dụng |
|---|---|---|
| `--lib-bg` | `#f1ede0` | Màu nền toàn trang (màu be giấy cổ điển, ấm áp) |
| `--lib-border` | `#ded8c6` | Đường viền panel, card, phân cách |
| `--lib-dark` | `#2b2a26` | Màu chữ chính, tiêu đề |
| `--lib-primary` | `#1d6fb8` | Màu thương hiệu, nút chính (`btn-primary`), icon nổi bật |
| `Header Background` | `#ffffff` | Nền thanh điều hướng trên cùng |
| `Footer Background` | `#e2dcc8` | Nền chân trang |

> [!IMPORTANT]
> **Không tự tiện viết mã màu hex tùy tiện vào thẻ style inline.** Luôn ưu tiên dùng các class của Bootstrap (`bg-light`, `text-muted`, `border`) kết hợp các biến CSS `--lib-*` và các class có sẵn trong `style.css`.

---

## 2. Khung Bố cục Chuẩn một Trang Thymeleaf

Mọi trang HTML trong `src/main/resources/templates/` (ngoại trừ các fragment) đều phải tuân theo cấu trúc khung sau:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<!-- 1. Tiêu đề trang được nhúng vào layout :: head -->
<head th:replace="~{fragments/layout :: head('Tiêu đề màn hình')}"></head>
<body>
<!-- 2. Navbar dùng chung -->
<div th:replace="~{fragments/layout :: navbar}"></div>

<!-- 3. Khu vực nội dung chính -->
<main class="container my-4">
    <!-- Khung panel nền trắng bo góc đồng bộ của hệ thống -->
    <div class="panel page-spacer">
        
        <!-- Header của trang -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h1 class="h4 mb-1">Tiêu đề màn hình</h1>
                <p class="text-muted small mb-0">Mô tả ngắn gọn chức năng của màn hình này.</p>
            </div>
            <!-- Nút hành động chính (nếu có) -->
            <div>
                <a class="btn btn-primary" th:href="@{/duong-dan/them-moi}">
                    <i class="bi bi-plus-lg me-1"></i> Thêm mới
                </a>
            </div>
        </div>

        <!-- 4. Thông báo kết quả (Alert message/error) -->
        <div th:replace="~{fragments/layout :: alerts}"></div>

        <!-- 5. Nội dung chính: Bảng, Form hoặc Card Grid -->
        <!-- ... -->

        <!-- 6. Phân trang nếu là danh sách -->
        <nav th:replace="~{fragments/layout :: pagination(${result}, @{/duong-dan-hien-tai(keyword=${keyword})})}"></nav>
    </div>
</main>

<!-- 7. Footer dùng chung -->
<div th:replace="~{fragments/layout :: footer}"></div>
</body>
</html>
```

---

## 3. Quy chuẩn các Thành phần UI (UI Components)

### 3.1. Bảng dữ liệu Quản trị (Data Tables)
Dành cho danh sách quản lý của Thủ thư / Admin (sách, bản sao, phiếu mượn, độc giả, phiếu nhập):

* **Class chuẩn:** `<table class="table table-hover align-middle">`
* **Header:** `<thead class="table-light">`
* **Căn lề:** Văn bản căn trái (`text-start`), số tiền/số lượng căn phải (`text-end`), trạng thái/hành động căn giữa (`text-center`).
* **Cột thao tác:** Dùng các nút nhỏ `.btn-sm` với icon rõ ràng:
  ```html
  <td class="text-center">
      <div class="btn-group btn-group-sm">
          <a class="btn btn-outline-secondary" th:href="@{/path/{id}/edit(id=${item.id})}" title="Chỉnh sửa">
              <i class="bi bi-pencil"></i>
          </a>
          <button class="btn btn-outline-danger" title="Xóa / Khóa">
              <i class="bi bi-trash"></i>
          </button>
      </div>
  </td>
  ```

### 3.2. Form Nhập liệu (Form Controls & Validation)
Dành cho các trang Thêm / Sửa dữ liệu:

* Đặt trong `<div class="card shadow-sm mx-auto" style="max-width: 720px;">`
* Sử dụng khoảng cách `mb-3` cho mỗi trường nhập.
* Luôn có nhãn `<label class="form-label fw-semibold">` rõ ràng.
* Trường bắt buộc phải có dấu sao đỏ: `<span class="text-danger">*</span>`.
* Hiển thị lỗi validation từ Spring (`BindingResult`):
  ```html
  <div class="mb-3">
      <label class="form-label fw-semibold" for="title">Tên sách <span class="text-danger">*</span></label>
      <input type="text" class="form-control" th:field="*{title}" id="title"
             th:classappend="${#fields.hasErrors('title')} ? 'is-invalid'">
      <div class="invalid-feedback" th:errors="*{title}">Lỗi tên sách</div>
  </div>
  ```
* Cụm nút bấm ở chân form:
  ```html
  <div class="d-flex gap-2 justify-content-end mt-4">
      <a class="btn btn-outline-secondary" th:href="@{/duong-dan/quay-lai}">Hủy bỏ</a>
      <button class="btn btn-primary" type="submit">
          <i class="bi bi-check-lg me-1"></i> Lưu thông tin
      </button>
  </div>
  ```

### 3.3. Nhãn trạng thái (Status Badges)
Các trạng thái enum hiển thị bằng `<span class="badge ...">` theo quy ước màu sau:

| Trạng thái Nghiệp vụ | Badge Class | Ý nghĩa / Ví dụ |
|---|---|---|
| Hoạt động / Sẵn sàng / Thành công | `bg-success` | `ACTIVE`, `AVAILABLE`, `RETURNED`, `PAID` |
| Chờ xử lý / Đang giữ chỗ / Cảnh báo | `bg-warning text-dark` | `PENDING`, `WAITING`, `RESERVED`, `HELD` |
| Đang mượn / Thông tin | `bg-primary` | `BORROWED`, `BORROWING` |
| Bị khóa / Quá hạn / Hỏng / Thất bại | `bg-danger` | `LOCKED`, `OVERDUE`, `DAMAGED`, `LOST` |
| Đã hủy / Đã thanh lý / Ẩn | `bg-secondary` | `CANCELLED`, `LIQUIDATED`, `DISPOSED`, `visible = false` |

Cú pháp hiển thị chuẩn trong Thymeleaf:
```html
<span class="badge bg-success" th:if="${book.visible}">Đang hiện</span>
<span class="badge bg-secondary" th:unless="${book.visible}">Đã ẩn</span>
```

### 3.4. Thẻ sách (Book Card & Cover)
* Ảnh bìa sách hiển thị bằng class `.book-cover` (đã quy định tỉ lệ và `object-fit: contain`).
* Khi không có ảnh bìa, dùng class `.book-cover.book-cover-empty` kèm chữ *"Chưa có ảnh bìa"*.

---

## 4. Quy chuẩn Tích hợp Menu Tài khoản theo Vai trò

Khi bạn hoàn thành một module màn hình, **bắt buộc** phải thêm mục truy cập vào đúng file menu phân quyền trong `src/main/resources/templates/fragments/`:

* **Dành cho Độc giả:** Chỉnh sửa [`menu-reader.html`](file:///e:/7.FALL26_ki7/HSF302/Group%20Project/LibraryManagementSystem/src/main/resources/templates/fragments/menu-reader.html)
* **Dành cho Thủ thư & Admin:** Chỉnh sửa [`menu-librarian.html`](file:///e:/7.FALL26_ki7/HSF302/Group%20Project/LibraryManagementSystem/src/main/resources/templates/fragments/menu-librarian.html)
* **Dành riêng cho Quản trị viên:** Chỉnh sửa [`menu-admin.html`](file:///e:/7.FALL26_ki7/HSF302/Group%20Project/LibraryManagementSystem/src/main/resources/templates/fragments/menu-admin.html)

**Định dạng chuẩn của một thẻ menu:**
```html
<a class="account-item" th:href="@{/librarian/books}">
    <span class="icon-circle"><i class="bi bi-journal-bookmark"></i></span> Quản lý đầu sách
</a>
```

---

## 5. Quy chuẩn Kiến trúc Backend kết nối Giao diện

1. **Phân quyền theo tiền tố URL:**
   * `/account/**` : Tất cả người dùng đã đăng nhập.
   * `/reader/**`  : Chỉ dành cho vai trò `READER`.
   * `/librarian/**`: Dành cho vai trò `LIBRARIAN` và `ADMIN`.
   * `/admin/**`   : Dành riêng cho `ADMIN`.
   * Các URL khác (`/books`, `/`, `/login`, `/register`): Công khai (Guest).
2. **Lấy người dùng hiện tại:**
   * Trong Controller: `SessionUser user = SessionUser.get(request);`
   * Trong Thymeleaf View: Biến `${currentUser}` luôn có sẵn tự động.
3. **Thông báo Flash Message sau khi Redirect:**
   * Thông báo thành công: `redirectAttributes.addFlashAttribute("message", "Thao tác thành công!");`
   * Thông báo lỗi: `redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi: ...");`
   * Giao diện chỉ cần `<div th:replace="~{fragments/layout :: alerts}"></div>` là tự hiển thị.
4. **Ném lỗi nghiệp vụ:**
   * Dùng `throw new BusinessException("Lý do lỗi bằng tiếng Việt rõ ràng");`
5. **Đồng bộ Enum:**
   * Toàn bộ enum trạng thái đặt tại `fu.libmanagement.common.enums`.
   * Luôn có `@Getter private final String label;` để hiển thị tiếng Việt lên UI bằng `${item.status.label}`.

