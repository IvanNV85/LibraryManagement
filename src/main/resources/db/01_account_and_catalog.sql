
IF DB_ID('libraryManagementdb') IS NULL
    CREATE DATABASE libraryManagementdb COLLATE Vietnamese_CI_AI;
GO

USE libraryManagementdb;
GO

/* ------------------------- Tài khoản ------------------------- */
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
    CONSTRAINT uq_users_email UNIQUE (email)
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
    CONSTRAINT fk_reader_cards_user FOREIGN KEY (user_id) REFERENCES users(id)
);
/* SQL Server chỉ cho 1 giá trị NULL trong cột UNIQUE, nên dùng filtered index để mã thẻ unique nhưng vẫn cho nhiều thẻ chưa có mã */
CREATE UNIQUE INDEX uq_reader_cards_code ON reader_cards(card_code) WHERE card_code IS NOT NULL;

/* Token xác thực email / đặt lại mật khẩu */
CREATE TABLE auth_tokens (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    token       VARCHAR(64) NOT NULL,
    token_type  VARCHAR(30) NOT NULL,                   -- EMAIL_VERIFY | PASSWORD_RESET
    expires_at  DATETIME2   NOT NULL,
    CONSTRAINT uq_auth_tokens_token UNIQUE (token),
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX ix_auth_tokens_user_type ON auth_tokens(user_id, token_type);

/* --------------------------- Danh mục sách --------------------------- */
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
GO
