package fu.libmanagement.common.util;

import fu.libmanagement.common.exception.BusinessException;

import java.util.regex.Pattern;

/** Các hàm kiểm tra dữ liệu nhập đơn giản, dùng chung. */
public final class ValidationUtil {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 150 && EMAIL.matcher(email).matches();
    }

    /** SĐT Việt Nam: 10 chữ số, bắt đầu bằng 0. */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    /** Cắt khoảng trắng hai đầu, null thành chuỗi rỗng. */
    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** Chuẩn hóa email trước khi tra cứu hoặc lưu: cắt khoảng trắng, chữ thường. */
    public static String normalizeEmail(String email) {
        return trim(email).toLowerCase();
    }

    /** Kiểm tra họ tên và SĐT (đã trim) dùng chung cho đăng ký và cập nhật hồ sơ. Sai thì ném BusinessException. */
    public static void checkNameAndPhone(String fullName, String phone) {
        if (fullName.length() < 2 || fullName.length() > 100) {
            throw new BusinessException("Họ tên phải từ 2 đến 100 ký tự");
        }
        if (!isValidPhone(phone)) {
            throw new BusinessException("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
        }
    }
}
