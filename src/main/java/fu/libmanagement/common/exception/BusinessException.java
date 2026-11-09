package fu.libmanagement.common.exception;

/**
 * Lỗi nghiệp vụ có thông báo hiển thị thẳng cho người dùng
 * (ví dụ: email trùng, mật khẩu yếu). Service ném ra, controller bắt và hiển thị.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
