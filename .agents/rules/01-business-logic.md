# Business Logic & Data Constraints

Tập hợp các quy tắc kiểm tra logic nghiệp vụ và ràng buộc dữ liệu được quy định trong tài liệu phân công:

## 1. Đầu sách & Bản sao (Books & Copies)
- **ISBN duy nhất:** Không có 2 đầu sách trùng ISBN trong bảng `books`. Khi thêm mới nếu trùng ISBN phải ném `BusinessException("Mã ISBN này đã tồn tại...")`.
- **Soft Delete:** Sách bị ẩn khi `visible = false`. Khách và độc giả chỉ thấy sách `visible = true`. Thủ thư thấy cả hai.
- **Bản sao vật lý:** Mỗi bản sao thuộc về một `Book`, có barcode và shelf location. Trạng thái bản sao tuân theo `BookCopyStatus`.

## 2. Kiểm tra điều kiện mượn sách (Borrowing Rules)
Trước khi cho phép độc giả gửi yêu cầu mượn hoặc thủ thư lập phiếu mượn tại quầy, hệ thống PHẢI kiểm tra đủ 3 điều kiện:
1. `ReaderCard.status == CardStatus.ACTIVE` VÀ `ReaderCard.expiryDate >= LocalDate.now()`.
2. Không có khoản phạt nào trong bảng `fines` có `status == FineStatus.UNPAID`.
3. Số lượng sách đang mượn (`loans` có trạng thái `BORROWED` hoặc `PENDING`) chưa vượt quá `MAX_BOOKS_PER_READER` (đọc từ bảng `settings`).

## 3. Cấu hình tập trung (Settings Service)
- Mọi giới hạn: số sách tối đa, số ngày mượn (`LOAN_DAYS`), số ngày gia hạn (`RENEW_DAYS`), tiền phạt mỗi ngày (`FINE_PER_DAY`) phải đọc từ bảng `settings`.
- Không hardcode các con số này trong code Java.

