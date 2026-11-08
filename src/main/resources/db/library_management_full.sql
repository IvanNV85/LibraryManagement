/* =====================================================================
   WEB QUẢN LÝ THƯ VIỆN TRỰC TUYẾN - CƠ SỞ DỮ LIỆU HOÀN CHỈNH (SQL Server)
   Gộp file 01_account_and_catalog.sql + các module còn lại.

   Module  -> bảng
     auth          : auth_tokens
     user          : users, reader_cards
     catalog       : categories, authors, books, book_authors
     inventory     : suppliers, purchase_orders, purchase_order_items, book_copies
     borrowing     : loans
     reservation   : reservations
     fine          : fines, payments
     engagement    : reviews, favorites
     notification  : notifications
     setting       : settings

   Chạy bằng SSMS / Azure Data Studio / sqlcmd (có dùng GO và USE).
   Nếu chạy bằng sqlcmd, thêm tham số -I để QUOTED_IDENTIFIER bật
   (bắt buộc với filtered index).
   ===================================================================== */
IF DB_ID('libraryManagementdb') IS NULL
    CREATE DATABASE libraryManagementdb COLLATE Vietnamese_CI_AI;
GO

USE libraryManagementdb;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

/* =====================================================================
   0. RESET (CHỈ DÙNG KHI PHÁT TRIỂN)
   Xóa toàn bộ bảng và view để dựng lại từ đầu. MẤT HẾT DỮ LIỆU.
   Đừng chạy script này trên database có dữ liệu thật.
   ===================================================================== */
DROP VIEW  IF EXISTS vw_reader_unpaid_fines;
DROP VIEW  IF EXISTS vw_book_availability;

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS fines;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS book_copies;
DROP TABLE IF EXISTS purchase_order_items;
DROP TABLE IF EXISTS purchase_orders;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS settings;
DROP TABLE IF EXISTS book_authors;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS authors;
DROP TABLE IF EXISTS publishers;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS auth_tokens;
DROP TABLE IF EXISTS reader_cards;
DROP TABLE IF EXISTS users;
GO

/* =====================================================================
   1. TÀI KHOẢN (module user + auth)
   ===================================================================== */
CREATE TABLE users (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    full_name          NVARCHAR(100) NOT NULL,
    email              NVARCHAR(150) NOT NULL,
    phone              NVARCHAR(15)  NULL,
    address            NVARCHAR(255) NULL,
    avatar_url         VARCHAR(255)  NULL,
    password_hash      VARCHAR(100)  NOT NULL,
    role               VARCHAR(20)   NOT NULL,          -- READER | LIBRARIAN | ADMIN
    status             VARCHAR(20)   NOT NULL,          -- ACTIVE | LOCKED
    email_verified     BIT           NOT NULL DEFAULT 0,
    failed_login_count INT           NOT NULL DEFAULT 0,
    locked_until       DATETIME2     NULL,
    created_at         DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uq_users_email  UNIQUE (email),
    CONSTRAINT ck_users_role   CHECK (role   IN ('READER','LIBRARIAN','ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','LOCKED'))
);

/* Thẻ thư viện: mã thẻ chỉ có sau khi thủ thư duyệt (UC23) nên cho phép NULL */
CREATE TABLE reader_cards (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    card_code    VARCHAR(20) NULL,
    status       VARCHAR(20) NOT NULL,                  -- PENDING | ACTIVE | LOCKED
    issued_date  DATE        NULL,
    expiry_date  DATE        NULL,
    CONSTRAINT uq_reader_cards_user UNIQUE (user_id),
    CONSTRAINT fk_reader_cards_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_reader_cards_status CHECK (status IN ('PENDING','ACTIVE','LOCKED'))
);
/* SQL Server chỉ cho 1 giá trị NULL trong cột UNIQUE, nên dùng filtered index để mã thẻ unique nhưng vẫn cho nhiều thẻ chưa có mã */
CREATE UNIQUE INDEX uq_reader_cards_code ON reader_cards(card_code) WHERE card_code IS NOT NULL;

/* Token xác thực email / đặt lại mật khẩu (so sánh phân biệt hoa thường) */
CREATE TABLE auth_tokens (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    token       VARCHAR(64) COLLATE Latin1_General_CS_AS NOT NULL,
    token_type  VARCHAR(30) NOT NULL,                   -- EMAIL_VERIFY | PASSWORD_RESET
    expires_at  DATETIME2   NOT NULL,
    CONSTRAINT uq_auth_tokens_token UNIQUE (token),
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_auth_tokens_type CHECK (token_type IN ('EMAIL_VERIFY','PASSWORD_RESET'))
);
CREATE INDEX ix_auth_tokens_user_type ON auth_tokens(user_id, token_type);

/* =====================================================================
   2. DANH MỤC SÁCH (module catalog)
   ===================================================================== */
CREATE TABLE categories (
    id    BIGINT IDENTITY(1,1) PRIMARY KEY,
    name  NVARCHAR(100) NOT NULL,
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE authors (
    id    BIGINT IDENTITY(1,1) PRIMARY KEY,
    name  NVARCHAR(150) NOT NULL
);
CREATE INDEX ix_authors_name ON authors(name);

CREATE TABLE books (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    isbn          VARCHAR(20)   NOT NULL,
    title         NVARCHAR(255) NOT NULL,
    publisher     NVARCHAR(150) NULL,
    publish_year  INT           NULL,
    description   NVARCHAR(MAX) NULL,
    cover_url     VARCHAR(255)  NULL,
    visible       BIT           NOT NULL DEFAULT 1,
    category_id   BIGINT        NULL,
    created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uq_books_isbn UNIQUE (isbn),
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories(id)
);
CREATE INDEX ix_books_title ON books(title);
CREATE INDEX ix_books_category ON books(category_id);

CREATE TABLE book_authors (
    book_id    BIGINT NOT NULL,
    author_id  BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    CONSTRAINT fk_book_authors_book   FOREIGN KEY (book_id)   REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_book_authors_author FOREIGN KEY (author_id) REFERENCES authors(id)
);
CREATE INDEX ix_book_authors_author ON book_authors(author_id);

/* =====================================================================
   3. CẤU HÌNH QUY ĐỊNH (module setting - UC25)
   Quy tắc dùng chung, ghi một nơi duy nhất, các module khác đọc qua SettingService
   ===================================================================== */
CREATE TABLE settings (
    setting_key    VARCHAR(50)   NOT NULL PRIMARY KEY,
    setting_value  NVARCHAR(100) NOT NULL,
    description    NVARCHAR(255) NULL,
    updated_at     DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    updated_by     BIGINT        NULL,
    CONSTRAINT fk_settings_user FOREIGN KEY (updated_by) REFERENCES users(id)
);

/* =====================================================================
   4. KHO SÁCH VÀ NHẬP SÁCH (module inventory - UC21, UC22)
   ===================================================================== */
CREATE TABLE suppliers (
    id       BIGINT IDENTITY(1,1) PRIMARY KEY,
    name     NVARCHAR(150) NOT NULL,
    phone    NVARCHAR(15)  NULL,
    email    NVARCHAR(150) NULL,
    address  NVARCHAR(255) NULL,
    CONSTRAINT uq_suppliers_name UNIQUE (name)
);

/* Phiếu nhập: thủ thư lập, admin phê duyệt */
CREATE TABLE purchase_orders (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    supplier_id  BIGINT        NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING | APPROVED | REJECTED | RECEIVED | CANCELLED
    note         NVARCHAR(255) NULL,
    created_by   BIGINT        NOT NULL,
    created_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    approved_by  BIGINT        NULL,
    approved_at  DATETIME2     NULL,
    received_at  DATETIME2     NULL,
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_po_created  FOREIGN KEY (created_by)  REFERENCES users(id),
    CONSTRAINT fk_po_approved FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT ck_po_status   CHECK (status IN ('PENDING','APPROVED','REJECTED','RECEIVED','CANCELLED'))
);
CREATE INDEX ix_po_status ON purchase_orders(status);

/* Dòng phiếu nhập: sách mới chưa có trong catalog thì book_id = NULL, giữ isbn và title để biên mục sau */
CREATE TABLE purchase_order_items (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    purchase_order_id  BIGINT         NOT NULL,
    book_id            BIGINT         NULL,
    isbn               VARCHAR(20)    NOT NULL,
    title              NVARCHAR(255)  NOT NULL,
    quantity           INT            NOT NULL,
    unit_price         DECIMAL(12,2)  NOT NULL,
    received_quantity  INT            NOT NULL DEFAULT 0,
    discrepancy_note   NVARCHAR(255)  NULL,                -- ghi chênh lệch / sách lỗi so với phiếu
    CONSTRAINT fk_poi_po   FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_poi_book FOREIGN KEY (book_id)           REFERENCES books(id),
    CONSTRAINT ck_poi_qty      CHECK (quantity > 0),
    CONSTRAINT ck_poi_price    CHECK (unit_price >= 0),
    CONSTRAINT ck_poi_received CHECK (received_quantity >= 0)
);
CREATE INDEX ix_poi_po ON purchase_order_items(purchase_order_id);

/* Bản sao sách: từng cuốn vật lý, có mã vạch riêng */
CREATE TABLE book_copies (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    book_id               BIGINT         NOT NULL,
    barcode               VARCHAR(30)    NOT NULL,
    shelf_location        NVARCHAR(50)   NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE | HELD | BORROWED | DAMAGED | LOST | DISPOSED
    price                 DECIMAL(12,2)  NULL,                        -- giá mua, dùng tính phạt hư hỏng / mất sách
    purchase_order_item_id BIGINT        NULL,
    note                  NVARCHAR(255)  NULL,
    created_at            DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uq_book_copies_barcode UNIQUE (barcode),
    CONSTRAINT fk_book_copies_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT fk_book_copies_poi  FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items(id),
    CONSTRAINT ck_book_copies_status CHECK (status IN ('AVAILABLE','HELD','BORROWED','DAMAGED','LOST','DISPOSED')),
    CONSTRAINT ck_book_copies_price  CHECK (price IS NULL OR price >= 0)
);
CREATE INDEX ix_book_copies_book_status ON book_copies(book_id, status);

/* =====================================================================
   5. MƯỢN - TRẢ (module borrowing - UC8, 10, 11, 16, 17, 18)
   Mỗi dòng là một cuốn sách. Độc giả mượn nhiều cuốn thì có nhiều dòng.
   Yêu cầu online (PENDING) chưa gán bản sao; khi thủ thư duyệt mới gán copy_id.
   Phiếu lập trực tiếp tại quầy (UC17) tạo thẳng ở trạng thái BORROWED, source = COUNTER.
   ===================================================================== */
CREATE TABLE loans (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    reader_id        BIGINT        NOT NULL,
    book_id          BIGINT        NOT NULL,
    copy_id          BIGINT        NULL,
    source           VARCHAR(10)   NOT NULL DEFAULT 'ONLINE',   -- ONLINE | COUNTER
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED | CANCELLED | EXPIRED | BORROWED | OVERDUE | RETURNED
    requested_at     DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    reviewed_by      BIGINT        NULL,                        -- thủ thư duyệt / từ chối
    reviewed_at      DATETIME2     NULL,
    reject_reason    NVARCHAR(255) NULL,
    pickup_deadline  DATETIME2     NULL,                        -- hạn đến nhận sách sau khi được duyệt
    issued_by        BIGINT        NULL,                        -- thủ thư giao sách
    borrowed_at      DATETIME2     NULL,
    due_date         DATE          NULL,                        -- ngày hẹn trả
    renew_count      INT           NOT NULL DEFAULT 0,
    returned_at      DATETIME2     NULL,                        -- ngày trả thực tế
    received_by      BIGINT        NULL,                        -- thủ thư nhận trả
    return_note      NVARCHAR(255) NULL,                        -- tình trạng sách khi trả
    CONSTRAINT fk_loans_reader   FOREIGN KEY (reader_id)   REFERENCES users(id),
    CONSTRAINT fk_loans_book     FOREIGN KEY (book_id)     REFERENCES books(id),
    CONSTRAINT fk_loans_copy     FOREIGN KEY (copy_id)     REFERENCES book_copies(id),
    CONSTRAINT fk_loans_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
    CONSTRAINT fk_loans_issuer   FOREIGN KEY (issued_by)   REFERENCES users(id),
    CONSTRAINT fk_loans_receiver FOREIGN KEY (received_by) REFERENCES users(id),
    CONSTRAINT ck_loans_source CHECK (source IN ('ONLINE','COUNTER')),
    CONSTRAINT ck_loans_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED','EXPIRED','BORROWED','OVERDUE','RETURNED')),
    CONSTRAINT ck_loans_renew  CHECK (renew_count >= 0)
);
CREATE INDEX ix_loans_reader_status ON loans(reader_id, status);
CREATE INDEX ix_loans_status_due    ON loans(status, due_date);
CREATE INDEX ix_loans_copy          ON loans(copy_id);
/* Một cuốn sách vật lý chỉ nằm trong một phiếu đang hiệu lực */
CREATE UNIQUE INDEX uq_loans_active_copy ON loans(copy_id)
    WHERE copy_id IS NOT NULL AND status IN ('APPROVED','BORROWED','OVERDUE');
/* Một độc giả không có hai yêu cầu / phiếu đang hiệu lực cho cùng một đầu sách */
CREATE UNIQUE INDEX uq_loans_active_reader_book ON loans(reader_id, book_id)
    WHERE status IN ('PENDING','APPROVED','BORROWED','OVERDUE');

/* =====================================================================
   6. ĐẶT TRƯỚC (module reservation - UC9)
   Hàng chờ theo created_at. Khi có sách trả về, người đầu hàng chuyển WAITING -> READY,
   được giữ copy_id đến hold_until; quá hạn thì EXPIRED và chuyển cho người kế tiếp.
   ===================================================================== */
CREATE TABLE reservations (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    reader_id     BIGINT      NOT NULL,
    book_id       BIGINT      NOT NULL,
    copy_id       BIGINT      NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'WAITING',  -- WAITING | READY | FULFILLED | CANCELLED | EXPIRED
    created_at    DATETIME2   NOT NULL DEFAULT SYSDATETIME(),
    notified_at   DATETIME2   NULL,
    hold_until    DATETIME2   NULL,
    CONSTRAINT fk_reservations_reader FOREIGN KEY (reader_id) REFERENCES users(id),
    CONSTRAINT fk_reservations_book   FOREIGN KEY (book_id)   REFERENCES books(id),
    CONSTRAINT fk_reservations_copy   FOREIGN KEY (copy_id)   REFERENCES book_copies(id),
    CONSTRAINT ck_reservations_status CHECK (status IN ('WAITING','READY','FULFILLED','CANCELLED','EXPIRED'))
);
CREATE INDEX ix_reservations_queue  ON reservations(book_id, status, created_at);
CREATE INDEX ix_reservations_reader ON reservations(reader_id, status);
/* Một độc giả chỉ đứng một chỗ trong hàng chờ của mỗi đầu sách */
CREATE UNIQUE INDEX uq_reservations_active ON reservations(reader_id, book_id)
    WHERE status IN ('WAITING','READY');

/* =====================================================================
   7. PHẠT VÀ THANH TOÁN (module fine - UC12, UC19)
   Phạt trễ = số ngày trễ x mức phạt (settings.FINE_PER_DAY).
   Độc giả bị chặn mượn khi còn phạt UNPAID (tính từ bảng này, không lưu cờ riêng).
   ===================================================================== */
CREATE TABLE fines (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    reader_id     BIGINT         NOT NULL,
    loan_id       BIGINT         NULL,
    fine_type     VARCHAR(20)    NOT NULL,                   -- OVERDUE | DAMAGED | LOST
    overdue_days  INT            NULL,
    amount        DECIMAL(12,2)  NOT NULL,
    description   NVARCHAR(255)  NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'UNPAID',  -- UNPAID | PAID | WAIVED
    created_by    BIGINT         NULL,
    created_at    DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    paid_at       DATETIME2      NULL,
    CONSTRAINT fk_fines_reader  FOREIGN KEY (reader_id)  REFERENCES users(id),
    CONSTRAINT fk_fines_loan    FOREIGN KEY (loan_id)    REFERENCES loans(id),
    CONSTRAINT fk_fines_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_fines_type    CHECK (fine_type IN ('OVERDUE','DAMAGED','LOST')),
    CONSTRAINT ck_fines_status  CHECK (status IN ('UNPAID','PAID','WAIVED')),
    CONSTRAINT ck_fines_amount  CHECK (amount > 0)
);
CREATE INDEX ix_fines_reader_status ON fines(reader_id, status);
/* Mỗi phiếu chỉ có một khoản phạt cho mỗi loại */
CREATE UNIQUE INDEX uq_fines_loan_type ON fines(loan_id, fine_type) WHERE loan_id IS NOT NULL;

CREATE TABLE payments (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    fine_id           BIGINT         NOT NULL,
    amount            DECIMAL(12,2)  NOT NULL,
    method            VARCHAR(10)    NOT NULL,                -- ONLINE | COUNTER
    status            VARCHAR(10)    NOT NULL DEFAULT 'SUCCESS', -- PENDING | SUCCESS | FAILED
    transaction_code  VARCHAR(64)    NULL,                    -- mã giao dịch cổng thanh toán
    received_by       BIGINT         NULL,                    -- thủ thư thu tiền tại quầy
    paid_at           DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_payments_fine     FOREIGN KEY (fine_id)     REFERENCES fines(id),
    CONSTRAINT fk_payments_receiver FOREIGN KEY (received_by) REFERENCES users(id),
    CONSTRAINT ck_payments_method   CHECK (method IN ('ONLINE','COUNTER')),
    CONSTRAINT ck_payments_status   CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    CONSTRAINT ck_payments_amount   CHECK (amount > 0)
);
CREATE INDEX ix_payments_fine ON payments(fine_id);

/* =====================================================================
   8. TƯƠNG TÁC (module engagement - UC13, UC14)
   ===================================================================== */
/* Chỉ độc giả đã từng mượn cuốn đó mới được đánh giá: kiểm tra ở tầng service */
CREATE TABLE reviews (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    book_id     BIGINT         NOT NULL,
    reader_id   BIGINT         NOT NULL,
    rating      TINYINT        NOT NULL,
    comment     NVARCHAR(1000) NULL,
    created_at  DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at  DATETIME2      NULL,
    CONSTRAINT uq_reviews_book_reader UNIQUE (book_id, reader_id),
    CONSTRAINT fk_reviews_book   FOREIGN KEY (book_id)   REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_reader FOREIGN KEY (reader_id) REFERENCES users(id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE favorites (
    reader_id   BIGINT    NOT NULL,
    book_id     BIGINT    NOT NULL,
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (reader_id, book_id),
    CONSTRAINT fk_favorites_reader FOREIGN KEY (reader_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_book   FOREIGN KEY (book_id)   REFERENCES books(id) ON DELETE CASCADE
);
CREATE INDEX ix_favorites_book ON favorites(book_id);

/* =====================================================================
   9. THÔNG BÁO (module notification - UC15)
   Kênh: trong web (is_read) và email (email_sent)
   ===================================================================== */
CREATE TABLE notifications (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT         NOT NULL,
    type        VARCHAR(30)    NOT NULL,
    title       NVARCHAR(150)  NOT NULL,
    content     NVARCHAR(500)  NULL,
    ref_id      BIGINT         NULL,                          -- id của phiếu mượn / đặt trước / sách liên quan
    is_read     BIT            NOT NULL DEFAULT 0,
    email_sent  BIT            NOT NULL DEFAULT 0,
    created_at  DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_notifications_type CHECK (type IN (
        'DUE_SOON','OVERDUE','LOAN_APPROVED','LOAN_REJECTED',
        'RESERVATION_READY','NEW_BOOK','FINE_CREATED','SYSTEM'))
);
CREATE INDEX ix_notifications_user ON notifications(user_id, is_read, created_at DESC);
GO

/* =====================================================================
   10. VIEW HỖ TRỢ
   ===================================================================== */
/* Số bản sao của từng đầu sách (không tính Mất / Thanh lý) và số bản đang sẵn sàng (UC6) */
CREATE VIEW vw_book_availability AS
SELECT b.id AS book_id,
       b.isbn,
       b.title,
       COUNT(c.id) AS total_copies,
       SUM(CASE WHEN c.status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_copies
FROM books b
LEFT JOIN book_copies c ON c.book_id = b.id AND c.status NOT IN ('LOST','DISPOSED')
GROUP BY b.id, b.isbn, b.title;
GO

/* Tổng tiền phạt chưa thanh toán của từng độc giả (dùng kiểm tra điều kiện mượn UC8, UC17) */
CREATE VIEW vw_reader_unpaid_fines AS
SELECT reader_id,
       COUNT(*)    AS unpaid_count,
       SUM(amount) AS unpaid_total
FROM fines
WHERE status = 'UNPAID'
GROUP BY reader_id;
GO

/* =====================================================================
   DỮ LIỆU MẪU (chỉ dùng để phát triển và demo)
   Tài khoản mẫu:
     admin@library.local      / Admin@123       (ADMIN)
     librarian@library.local  / Librarian@123   (LIBRARIAN)
     reader@library.local     / Reader@123      (READER, thẻ đã duyệt)
   ===================================================================== */
INSERT INTO users (full_name, email, phone, password_hash, role, status, email_verified) VALUES
(N'Quản trị viên', 'admin@library.local',     '0900000001', '$2b$10$jnun0cResYWZPQplAu2w1uU3qmKtWdn85oomOlU3Q/AXP14NU3TNu', 'ADMIN',     'ACTIVE', 1),
(N'Thủ thư mẫu',   'librarian@library.local', '0900000002', '$2b$10$Y8cE.vj31E.KCbIVr7JLhOx.XOXBQ1jrumEaQDohHQIW6AZlKHGQm', 'LIBRARIAN', 'ACTIVE', 1),
(N'Độc giả mẫu',   'reader@library.local',    '0900000003', '$2b$10$500Tg2LNjzkItVF5JRS9Ie3JhSm5hnCcYHdg0z5ph8wI5PvTgDneS', 'READER',    'ACTIVE', 1);

INSERT INTO reader_cards (user_id, card_code, status, issued_date, expiry_date)
SELECT id, 'LIB000001', 'ACTIVE', CAST(SYSDATETIME() AS DATE), DATEADD(YEAR, 1, CAST(SYSDATETIME() AS DATE))
FROM users WHERE email = 'reader@library.local';

INSERT INTO categories (name) VALUES
(N'Văn học Việt Nam'), (N'Văn học nước ngoài'), (N'Công nghệ thông tin'), (N'Kỹ năng sống');

INSERT INTO authors (name) VALUES
(N'Tô Hoài'), (N'Vũ Trọng Phụng'), (N'Nguyễn Du'), (N'Ngô Tất Tố'), (N'Paulo Coelho'),
(N'Robert C. Martin'), (N'Joshua Bloch'), (N'Craig Walls'), (N'Andres Sacco'), (N'Dale Carnegie'),
(N'Nguyễn Văn Mẫu');

INSERT INTO publishers (name) VALUES
(N'NXB Kim Đồng'), (N'NXB Trẻ'), (N'NXB Giáo dục Việt Nam'), (N'NXB Văn học'),
(N'NXB Tổng hợp TP.HCM'), (N'NXB Tổng hợp'), (N'NXB Lao Động'), (N'NXB Hội Nhà Văn'),
(N'NXB Thế Giới'), (N'NXB Phụ Nữ Việt Nam'), (N'NXB Tri Thức'), (N'NXB Thông tin và Truyền thông'),
(N'NXB Mẫu'), (N'Prentice Hall'), (N'Addison-Wesley'), (N'Manning'), (N'Apress'),
(N'O''Reilly Media'), (N'Packt Publishing'), (N'McGraw-Hill Education'),
(N'Pearson'), (N'Wiley'), (N'MIT Press'), (N'No Starch Press');

/* Ảnh bìa nằm trong src/main/resources/static/images/covers/ (được commit cùng code).
   ISBN bên dưới là ISBN mẫu (không phải ISBN thật), trừ cuốn "Beginning Spring Data" */
INSERT INTO books (isbn, title, publisher, publish_year, description, cover_url, category_id) VALUES
('9786040000011', N'Dế Mèn phiêu lưu ký',   N'NXB Kim Đồng', 2019, N'Truyện thiếu nhi kinh điển.', '/images/covers/9786040000011.svg', (SELECT id FROM categories WHERE name = N'Văn học Việt Nam')),
('9786040000012', N'Số đỏ',                   N'NXB Văn học',  2018, N'Tiểu thuyết trào phúng.', '/images/covers/9786040000012.svg', (SELECT id FROM categories WHERE name = N'Văn học Việt Nam')),
('9786040000013', N'Truyện Kiều',             N'NXB Văn học',  2020, N'Truyện thơ Nôm.', '/images/covers/9786040000013.svg', (SELECT id FROM categories WHERE name = N'Văn học Việt Nam')),
('9786040000014', N'Tắt đèn',                 N'NXB Văn học',  2017, N'Tiểu thuyết hiện thực phê phán.', '/images/covers/9786040000014.svg', (SELECT id FROM categories WHERE name = N'Văn học Việt Nam')),
('9786040000015', N'Nhà giả kim',             N'NXB Văn học',  2020, N'Tiểu thuyết nước ngoài được dịch rộng rãi.', '/images/covers/9786040000015.svg', (SELECT id FROM categories WHERE name = N'Văn học nước ngoài')),
('9786040000016', N'Clean Code',              N'Prentice Hall', 2008, N'Cách viết mã sạch và dễ bảo trì.', '/images/covers/9786040000016.svg', (SELECT id FROM categories WHERE name = N'Công nghệ thông tin')),
('9786040000017', N'Effective Java',          N'Addison-Wesley', 2018, N'Các thực hành tốt khi lập trình Java.', '/images/covers/9786040000017.svg', (SELECT id FROM categories WHERE name = N'Công nghệ thông tin')),
('9786040000018', N'Spring in Action',        N'Manning',      2022, N'Xây dựng ứng dụng với Spring Framework.', '/images/covers/9786040000018.svg', (SELECT id FROM categories WHERE name = N'Công nghệ thông tin')),
('1484287630',    N'Beginning Spring Data',   N'Apress',       2022, N'Data access và persistence với Spring Framework 6 và Boot 3.', '/images/covers/1484287630.svg', (SELECT id FROM categories WHERE name = N'Công nghệ thông tin')),
('9786040000020', N'Đắc nhân tâm',            N'NXB Tổng hợp', 2016, N'Nghệ thuật giao tiếp và ứng xử.', '/images/covers/9786040000020.svg', (SELECT id FROM categories WHERE name = N'Kỹ năng sống')),
('9786040000021', N'Lập trình Java cơ bản',   N'NXB Mẫu',      2021, N'Giáo trình mẫu để thử tìm kiếm không dấu.', '/images/covers/9786040000021.svg', (SELECT id FROM categories WHERE name = N'Công nghệ thông tin')),
('9786040000022', N'Lập trình Spring Boot',   N'NXB Mẫu',      2023, N'Giáo trình mẫu để thử phân trang.', '/images/covers/9786040000022.svg', (SELECT id FROM categories WHERE name = N'Công nghệ thông tin'));

/* Gán tác giả cho sách (hai cuốn giáo trình mẫu dùng chung một tác giả giả định để thử tìm theo tác giả) */
INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id FROM (VALUES
    ('9786040000011', N'Tô Hoài'),
    ('9786040000012', N'Vũ Trọng Phụng'),
    ('9786040000013', N'Nguyễn Du'),
    ('9786040000014', N'Ngô Tất Tố'),
    ('9786040000015', N'Paulo Coelho'),
    ('9786040000016', N'Robert C. Martin'),
    ('9786040000017', N'Joshua Bloch'),
    ('9786040000018', N'Craig Walls'),
    ('1484287630',    N'Andres Sacco'),
    ('9786040000020', N'Dale Carnegie'),
    ('9786040000021', N'Nguyễn Văn Mẫu'),
    ('9786040000022', N'Nguyễn Văn Mẫu')
) AS m(isbn, author_name)
JOIN books b ON b.isbn = m.isbn
JOIN authors a ON a.name = m.author_name;

/* Quy định chung (UC25) */
INSERT INTO settings (setting_key, setting_value, description) VALUES
('MAX_BOOKS_PER_READER',   N'5',    N'Số sách tối đa một độc giả được mượn cùng lúc'),
('LOAN_DAYS',              N'14',   N'Số ngày mượn mặc định'),
('MAX_RENEWALS',           N'2',    N'Số lần gia hạn tối đa cho mỗi phiếu'),
('RENEW_DAYS',             N'7',    N'Số ngày được cộng thêm mỗi lần gia hạn'),
('FINE_PER_DAY',           N'2000', N'Mức phạt trả trễ (VND mỗi ngày mỗi cuốn)'),
('LOST_FINE_PERCENT',      N'100',  N'Phạt mất sách, tính theo % giá sách'),
('PICKUP_HOLD_DAYS',       N'3',    N'Số ngày giữ sách sau khi yêu cầu mượn được duyệt'),
('RESERVATION_HOLD_DAYS',  N'3',    N'Số ngày giữ sách cho người đặt trước (N ngày)'),
('DUE_REMINDER_DAYS',      N'2',    N'Nhắc trước hạn trả bao nhiêu ngày'),
('CARD_VALID_MONTHS',      N'12',   N'Thời hạn thẻ thư viện (tháng)');

/* Nhà cung cấp và một phiếu nhập mẫu đã được duyệt (UC22) */
INSERT INTO suppliers (name, phone, email, address) VALUES
(N'Công ty Sách Phương Nam', '0280000001', 'sales@phuongnam.example', N'TP. Hồ Chí Minh'),
(N'Nhà sách Fahasa Mẫu',     '0280000002', 'order@fahasa.example',    N'TP. Hồ Chí Minh');

INSERT INTO purchase_orders (supplier_id, status, note, created_by, approved_by, approved_at)
SELECT (SELECT id FROM suppliers WHERE name = N'Công ty Sách Phương Nam'),
       'APPROVED', N'Phiếu nhập mẫu để demo luồng bổ sung sách',
       (SELECT id FROM users WHERE email = 'librarian@library.local'),
       (SELECT id FROM users WHERE email = 'admin@library.local'),
       SYSDATETIME();

INSERT INTO purchase_order_items (purchase_order_id, book_id, isbn, title, quantity, unit_price) VALUES
((SELECT TOP 1 id FROM purchase_orders ORDER BY id), NULL, '9786040000099', N'Thiết kế hệ thống', 3, 120000),
((SELECT TOP 1 id FROM purchase_orders ORDER BY id),
 (SELECT id FROM books WHERE isbn = '9786040000016'), '9786040000016', N'Clean Code', 2, 250000);

/* Mỗi đầu sách có 2 bản sao mẫu, sẵn sàng cho mượn */
INSERT INTO book_copies (book_id, barcode, shelf_location, status, price)
SELECT b.id,
       'BC' + RIGHT('00000' + CAST(ROW_NUMBER() OVER (ORDER BY b.id, n.n) AS VARCHAR(10)), 5),
       N'Kệ ' + CAST(ISNULL(b.category_id, 0) AS NVARCHAR(10)),
       'AVAILABLE',
       85000
FROM books b
CROSS JOIN (VALUES (1), (2)) AS n(n);

/* Phiếu mượn mẫu: một phiếu đang mượn, một phiếu đã trả đúng hạn */
INSERT INTO loans (reader_id, book_id, copy_id, source, status, requested_at,
                   reviewed_by, reviewed_at, issued_by, borrowed_at, due_date)
SELECT (SELECT id FROM users WHERE email = 'reader@library.local'),
       b.id,
       (SELECT TOP 1 c.id FROM book_copies c WHERE c.book_id = b.id ORDER BY c.id),
       'ONLINE', 'BORROWED', DATEADD(DAY, -8, SYSDATETIME()),
       (SELECT id FROM users WHERE email = 'librarian@library.local'), DATEADD(DAY, -8, SYSDATETIME()),
       (SELECT id FROM users WHERE email = 'librarian@library.local'), DATEADD(DAY, -7, SYSDATETIME()),
       CAST(DATEADD(DAY, 7, SYSDATETIME()) AS DATE)
FROM books b WHERE b.isbn = '9786040000016';

UPDATE c SET status = 'BORROWED'
FROM book_copies c
JOIN loans l ON l.copy_id = c.id AND l.status = 'BORROWED';

INSERT INTO loans (reader_id, book_id, copy_id, source, status, requested_at,
                   reviewed_by, reviewed_at, issued_by, borrowed_at, due_date, returned_at, received_by)
SELECT (SELECT id FROM users WHERE email = 'reader@library.local'),
       b.id,
       (SELECT TOP 1 c.id FROM book_copies c WHERE c.book_id = b.id ORDER BY c.id),
       'COUNTER', 'RETURNED', DATEADD(DAY, -30, SYSDATETIME()),
       NULL, NULL,
       (SELECT id FROM users WHERE email = 'librarian@library.local'), DATEADD(DAY, -30, SYSDATETIME()),
       CAST(DATEADD(DAY, -16, SYSDATETIME()) AS DATE), DATEADD(DAY, -20, SYSDATETIME()),
       (SELECT id FROM users WHERE email = 'librarian@library.local')
FROM books b WHERE b.isbn = '9786040000015';

/* Tương tác mẫu: độc giả đã mượn "Nhà giả kim" nên được đánh giá, và yêu thích một cuốn */
INSERT INTO reviews (book_id, reader_id, rating, comment)
SELECT b.id, (SELECT id FROM users WHERE email = 'reader@library.local'), 5, N'Sách hay, dễ đọc.'
FROM books b WHERE b.isbn = '9786040000015';

INSERT INTO favorites (reader_id, book_id)
SELECT (SELECT id FROM users WHERE email = 'reader@library.local'), b.id
FROM books b WHERE b.isbn = '9786040000017';

INSERT INTO notifications (user_id, type, title, content)
SELECT id, 'SYSTEM', N'Chào mừng đến thư viện', N'Thẻ thư viện của bạn đã được duyệt, bạn có thể bắt đầu mượn sách.'
FROM users WHERE email = 'reader@library.local';
GO
