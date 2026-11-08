USE libraryManagementdb;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

/* =====================================================================
   BƯỚC 1: BỔ SUNG CỘT CHO BẢNG LOANS (NẾU CƠ SỞ DỮ LIỆU CŨ THIẾU CỘT)
   ===================================================================== */
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'loans' AND COLUMN_NAME = 'reviewed_by')
BEGIN
ALTER TABLE loans ADD reviewed_by BIGINT NULL;
PRINT N'Đã bổ sung cột reviewed_by vào bảng loans.';
END
GO

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'loans' AND COLUMN_NAME = 'reviewed_at')
BEGIN
ALTER TABLE loans ADD reviewed_at DATETIME2 NULL;
PRINT N'Đã bổ sung cột reviewed_at vào bảng loans.';
END
GO

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'loans' AND COLUMN_NAME = 'pickup_deadline')
BEGIN
ALTER TABLE loans ADD pickup_deadline DATETIME2 NULL;
PRINT N'Đã bổ sung cột pickup_deadline vào bảng loans.';
END
GO

/* Đồng bộ dữ liệu nếu database trước đó dùng tên cột approved_by / approved_at */
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'loans' AND COLUMN_NAME = 'approved_by')
BEGIN
    EXEC('UPDATE loans SET reviewed_by = approved_by WHERE reviewed_by IS NULL AND approved_by IS NOT NULL');
END
GO

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'loans' AND COLUMN_NAME = 'approved_at')
BEGIN
    EXEC('UPDATE loans SET reviewed_at = approved_at WHERE reviewed_at IS NULL AND approved_at IS NOT NULL');
END
GO

/* =====================================================================
   BƯỚC 2: SEED DỮ LIỆU MẪU KHOẢN PHẠT (UC19)
   ===================================================================== */
DECLARE @librarianId BIGINT = (SELECT TOP 1 id FROM users WHERE role = 'LIBRARIAN');
IF @librarianId IS NULL SET @librarianId = (SELECT TOP 1 id FROM users WHERE role = 'ADMIN');

-- 1. Tìm hoặc tạo phiếu mượn quá hạn (OVERDUE) và gán phạt UNPAID
DECLARE @overdueLoanId BIGINT = (SELECT TOP 1 id FROM loans WHERE status = 'OVERDUE');
DECLARE @overdueReaderId BIGINT = (SELECT TOP 1 reader_id FROM loans WHERE id = @overdueLoanId);

IF @overdueLoanId IS NULL
BEGIN
    DECLARE @sampleReaderId BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'reader@library.local');
    DECLARE @sampleCopyId BIGINT = (SELECT TOP 1 id FROM book_copies WHERE status = 'AVAILABLE');
    DECLARE @sampleBookId BIGINT = (SELECT book_id FROM book_copies WHERE id = @sampleCopyId);

    IF @sampleReaderId IS NOT NULL AND @sampleCopyId IS NOT NULL
    BEGIN
        INSERT INTO loans (reader_id, book_id, copy_id, source, status, requested_at,
                           reviewed_by, reviewed_at, issued_by, borrowed_at, due_date)
        VALUES (@sampleReaderId, @sampleBookId, @sampleCopyId, 'ONLINE', 'OVERDUE',
                DATEADD(DAY, -20, SYSDATETIME()), @librarianId, DATEADD(DAY, -20, SYSDATETIME()),
                @librarianId, DATEADD(DAY, -19, SYSDATETIME()), CAST(DATEADD(DAY, -5, SYSDATETIME()) AS DATE));

        SET @overdueLoanId = SCOPE_IDENTITY();
        SET @overdueReaderId = @sampleReaderId;

        UPDATE book_copies SET status = 'BORROWED' WHERE id = @sampleCopyId;
    END
END

IF @overdueLoanId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM fines WHERE loan_id = @overdueLoanId)
BEGIN
    INSERT INTO fines (loan_id, reader_id, fine_type, overdue_days, amount, status, description, created_by, created_at)
    VALUES (@overdueLoanId, @overdueReaderId, 'OVERDUE', 5, 10000.00, 'UNPAID', N'Trả trễ hạn 5 ngày (Mức phạt 2.000đ/ngày)', @librarianId, SYSDATETIME());
    PRINT N'Đã tạo khoản phạt UNPAID 10,000 đ cho phiếu mượn #' + CAST(@overdueLoanId AS NVARCHAR(10));
END

-- 2. Tìm hoặc gán phạt PAID cho phiếu mượn đã trả (RETURNED)
DECLARE @returnedLoanId BIGINT = (SELECT TOP 1 id FROM loans WHERE status = 'RETURNED' AND id <> ISNULL(@overdueLoanId, 0));
DECLARE @returnedReaderId BIGINT = (SELECT TOP 1 reader_id FROM loans WHERE id = @returnedLoanId);

IF @returnedLoanId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM fines WHERE loan_id = @returnedLoanId)
BEGIN
    DECLARE @paidFineId BIGINT;
    INSERT INTO fines (loan_id, reader_id, fine_type, overdue_days, amount, status, description, created_by, created_at, paid_at)
    VALUES (@returnedLoanId, @returnedReaderId, 'OVERDUE', 3, 6000.00, 'PAID', N'Trả trễ hạn 3 ngày (Đã thu tại quầy)', @librarianId, DATEADD(DAY, -2, SYSDATETIME()), DATEADD(DAY, -2, SYSDATETIME()));

    SET @paidFineId = SCOPE_IDENTITY();

    -- Ghi nhận bản ghi thanh toán trong bảng payments
    IF NOT EXISTS (SELECT 1 FROM payments WHERE fine_id = @paidFineId)
    BEGIN
        INSERT INTO payments (fine_id, amount, method, status, transaction_code, received_by, paid_at)
        VALUES (@paidFineId, 6000.00, 'COUNTER', 'SUCCESS', 'PAY-SEED-' + CAST(@paidFineId AS VARCHAR(10)), @librarianId, DATEADD(DAY, -2, SYSDATETIME()));
    END
    PRINT N'Đã tạo khoản phạt PAID 6,000 đ cho phiếu mượn #' + CAST(@returnedLoanId AS NVARCHAR(10));
END
GO
