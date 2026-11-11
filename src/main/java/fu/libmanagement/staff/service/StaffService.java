package fu.libmanagement.staff.service;

import fu.libmanagement.auth.service.PasswordHelper;
import fu.libmanagement.common.enums.AccountStatus;
import fu.libmanagement.common.enums.Role;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.staff.dto.StaffForm;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

/** Nghiệp vụ quản lý nhân viên, dùng chung entity User và chỉ thao tác với vai trò LIBRARIAN. */
@Service
@RequiredArgsConstructor
public class StaffService {
    private final UserRepository users;
    private final PasswordHelper passwords;

    /** Tìm thủ thư theo họ tên/email, không phân biệt hoa thường; mỗi trang 10 người, trang đầu là 0. */
    @Transactional(readOnly = true)
    public Page<User> search(String keyword, int page) {
        String text = ValidationUtil.trim(keyword).toLowerCase(Locale.ROOT);
        return users.searchStaff(Role.LIBRARIAN, "%" + text + "%", PageRequest.of(Math.max(page, 0), 10));
    }

    /** Lấy thủ thư theo ID; báo lỗi nếu không tồn tại hoặc ID thuộc tài khoản có vai trò khác. */
    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).filter(u -> u.getRole() == Role.LIBRARIAN)
                .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
    }

    /**
     * Kiểm tra mật khẩu và thông tin cá nhân, băm mật khẩu bằng BCrypt rồi lưu thủ thư mới.
     * Vai trò LIBRARIAN và trạng thái ACTIVE do server gán, không nhận từ form.
     */
    @Transactional
    public User create(StaffForm form) {
        passwords.validateNewPassword(form.getPassword(), form.getConfirmPassword());
        User user = new User();
        user.setRole(Role.LIBRARIAN);
        user.setStatus(AccountStatus.ACTIVE);
        user.setPasswordHash(passwords.hash(form.getPassword()));
        updateFields(user, form);
        // Flush ngay để bắt lỗi ràng buộc tại database, kể cả khi email bị đăng ký đồng thời.
        try { return users.saveAndFlush(user); }
        catch (DataIntegrityViolationException e) { throw new BusinessException("Email này đã được đăng ký"); }
    }

    /** Cập nhật thông tin cá nhân của thủ thư; giữ nguyên mật khẩu, vai trò và trạng thái tài khoản. */
    @Transactional
    public void update(Long id, StaffForm form) {
        User user = get(id);
        updateFields(user, form);
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException e) { throw new BusinessException("Email này đã được đăng ký"); }
    }

    /** Đổi trạng thái tài khoản thay vì xóa bản ghi User, giữ các liên kết dữ liệu nghiệp vụ. */
    @Transactional
    public void setLocked(Long id, boolean locked) {
        // Entity được quản lý trong transaction; JPA tự lưu thay đổi khi commit (dirty checking).
        get(id).setStatus(locked ? AccountStatus.LOCKED : AccountStatus.ACTIVE);
    }

    /**
     * Dùng chung cho thêm/sửa: chuẩn hóa dữ liệu, kiểm tra họ tên, email, điện thoại và địa chỉ.
     * Email phải duy nhất trên toàn bộ tài khoản; điện thoại/địa chỉ trống được lưu thành null.
     */
    private void updateFields(User user, StaffForm form) {
        String name = ValidationUtil.trim(form.getFullName());
        String email = ValidationUtil.normalizeEmail(form.getEmail());
        String phone = ValidationUtil.trim(form.getPhone());
        String address = ValidationUtil.trim(form.getAddress());
        ValidationUtil.checkFullName(name);
        if (!ValidationUtil.isValidEmail(email)) throw new BusinessException("Email không hợp lệ");
        ValidationUtil.checkPhoneIfPresent(phone);
        if (address.length() > 255) throw new BusinessException("Địa chỉ tối đa 255 ký tự");
        // Cho phép giữ email của chính tài khoản đang sửa, chặn email thuộc tài khoản khác.
        users.findByEmail(email).filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> { throw new BusinessException("Email này đã được đăng ký"); });
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setAddress(address.isEmpty() ? null : address);
    }
}
