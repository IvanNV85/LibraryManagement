# Library Management System - UI Code Snippets Reference

Các đoạn mã mẫu chuẩn (copy-paste ready) cho Thymeleaf và Bootstrap 5 trong dự án.

---

## 1. Mẫu Bảng Quản trị (Data Table với Search & Actions)

```html
<div class="table-responsive">
    <table class="table table-hover align-middle">
        <thead class="table-light">
            <tr>
                <th style="width: 60px;">#</th>
                <th>Thông tin chính</th>
                <th>Phân loại</th>
                <th>Thời gian</th>
                <th class="text-center" style="width: 130px;">Trạng thái</th>
                <th class="text-center" style="width: 140px;">Thao tác</th>
            </tr>
        </thead>
        <tbody>
            <tr th:if="${page.empty}">
                <td colspan="6" class="text-center py-4 text-muted">
                    <i class="bi bi-inbox fs-3 d-block mb-1"></i>
                    Không có dữ liệu phù hợp.
                </td>
            </tr>
            <tr th:each="item, stat : ${page.content}">
                <td class="text-muted" th:text="${stat.count + page.number * page.size}">1</td>
                <td>
                    <div class="fw-semibold text-dark" th:text="${item.name}">Tên đối tượng</div>
                    <small class="text-muted" th:text="${item.code}">Mã số</small>
                </td>
                <td th:text="${item.category != null ? item.category.name : '-'}">Phân loại</td>
                <td>
                    <small class="text-muted" th:text="${#temporals.format(item.createdAt, 'dd/MM/yyyy HH:mm')}">01/01/2026</small>
                </td>
                <td class="text-center">
                    <span class="badge bg-success" th:if="${item.status.name() == 'ACTIVE'}" th:text="${item.status.label}">Hoạt động</span>
                    <span class="badge bg-warning text-dark" th:if="${item.status.name() == 'PENDING'}" th:text="${item.status.label}">Chờ duyệt</span>
                    <span class="badge bg-danger" th:if="${item.status.name() == 'LOCKED'}" th:text="${item.status.label}">Bị khóa</span>
                </td>
                <td class="text-center">
                    <div class="btn-group btn-group-sm">
                        <a class="btn btn-outline-secondary" th:href="@{/librarian/items/{id}/edit(id=${item.id})}" title="Chỉnh sửa">
                            <i class="bi bi-pencil"></i>
                        </a>
                        <form th:action="@{/librarian/items/{id}/toggle(id=${item.id})}" method="post" class="d-inline"
                              onsubmit="return confirm('Bạn có chắc muốn thay đổi trạng thái mục này?');">
                            <button type="submit" class="btn btn-outline-danger" title="Khóa / Xóa">
                                <i class="bi bi-power"></i>
                            </button>
                        </form>
                    </div>
                </td>
            </tr>
        </tbody>
    </table>
</div>
```

---

## 2. Mẫu Form Nhập liệu (Create / Edit Form)

```html
<div class="card shadow-sm mx-auto" style="max-width: 760px;">
    <div class="card-body p-4">
        <h4 class="card-title mb-1" th:text="${item.id == null ? 'Thêm mới' : 'Chỉnh sửa'}">Tiêu đề Form</h4>
        <p class="text-muted small mb-4">Vui lòng điền đầy đủ các trường thông tin có đánh dấu sao (*).</p>

        <form th:action="${item.id == null ? '/librarian/items/new' : '/librarian/items/' + item.id + '/edit'}"
              th:object="${form}" method="post">

            <!-- Ô Text bình thường -->
            <div class="mb-3">
                <label class="form-label fw-semibold" for="title">Tiêu đề <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="title" th:field="*{title}"
                       th:classappend="${#fields.hasErrors('title')} ? 'is-invalid'" placeholder="Nhập tiêu đề...">
                <div class="invalid-feedback" th:errors="*{title}">Lỗi tiêu đề</div>
            </div>

            <!-- Dropdown Select -->
            <div class="mb-3">
                <label class="form-label fw-semibold" for="categoryId">Thể loại <span class="text-danger">*</span></label>
                <select class="form-select" id="categoryId" th:field="*{categoryId}"
                        th:classappend="${#fields.hasErrors('categoryId')} ? 'is-invalid'">
                    <option value="">-- Chọn thể loại --</option>
                    <option th:each="cat : ${categories}" th:value="${cat.id}" th:text="${cat.name}"></option>
                </select>
                <div class="invalid-feedback" th:errors="*{categoryId}">Vui lòng chọn thể loại</div>
            </div>

            <!-- Textarea -->
            <div class="mb-3">
                <label class="form-label fw-semibold" for="description">Mô tả chi tiết</label>
                <textarea class="form-control" id="description" th:field="*{description}" rows="4"
                          placeholder="Mô tả nội dung..."></textarea>
            </div>

            <!-- Nút bấm hành động -->
            <div class="d-flex justify-content-end gap-2 pt-3 border-top">
                <a class="btn btn-outline-secondary" th:href="@{/librarian/items}">Hủy bỏ</a>
                <button type="submit" class="btn btn-primary">
                    <i class="bi bi-save me-1"></i> Lưu thông tin
                </button>
            </div>
        </form>
    </div>
</div>
```

---

## 3. Mẫu Thanh Tìm kiếm và Bộ lọc Trên trang Danh sách

```html
<div class="row g-2 align-items-center mb-3">
    <div class="col-12 col-md-6 col-lg-4">
        <form th:action="@{/duong-dan}" method="get" class="d-flex gap-2">
            <input type="text" class="form-control" name="keyword" th:value="${keyword}"
                   placeholder="Tìm kiếm từ khóa...">
            <button class="btn btn-outline-primary" type="submit">
                <i class="bi bi-search"></i>
            </button>
        </form>
    </div>
</div>
```

