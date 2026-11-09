USE libraryManagementdb;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

/* =====================================================================
   TẠO BẢNG PUBLISHERS VÀ SEED DỮ LIỆU NHÀ XUẤT BẢN
   ===================================================================== */

-- 1. Tạo bảng publishers nếu chưa tồn tại
IF OBJECT_ID('publishers', 'U') IS NULL
BEGIN
    CREATE TABLE publishers (
        id    BIGINT IDENTITY(1,1) PRIMARY KEY,
        name  NVARCHAR(150) NOT NULL,
        CONSTRAINT uq_publishers_name UNIQUE (name)
    );
    CREATE INDEX ix_publishers_name ON publishers(name);
    PRINT N'Bảng publishers đã được tạo thành công.';
END
ELSE
BEGIN
    PRINT N'Bảng publishers đã tồn tại.';
END
GO

-- 2. Chèn dữ liệu mẫu cho các nhà xuất bản phổ biến (dùng MERGE để chống trùng lặp)
MERGE INTO publishers AS target
USING (VALUES
    (N'NXB Kim Đồng'),
    (N'NXB Trẻ'),
    (N'NXB Giáo dục Việt Nam'),
    (N'NXB Văn học'),
    (N'NXB Tổng hợp TP.HCM'),
    (N'NXB Tổng hợp'),
    (N'NXB Lao Động'),
    (N'NXB Hội Nhà Văn'),
    (N'NXB Thế Giới'),
    (N'NXB Phụ Nữ Việt Nam'),
    (N'NXB Tri Thức'),
    (N'NXB Thông tin và Truyền thông'),
    (N'NXB Mẫu'),
    (N'Prentice Hall'),
    (N'Addison-Wesley'),
    (N'Manning'),
    (N'Apress'),
    (N'O''Reilly Media'),
    (N'Packt Publishing'),
    (N'McGraw-Hill Education'),
    (N'Pearson'),
    (N'Wiley'),
    (N'MIT Press'),
    (N'No Starch Press')
) AS source (name)
ON target.name = source.name
WHEN NOT MATCHED THEN
    INSERT (name) VALUES (source.name);
GO

PRINT N'Dữ liệu nhà xuất bản đã được chèn/cập nhật thành công.';
GO

