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

@Service
@RequiredArgsConstructor
public class StaffService {
    private final UserRepository users;
    private final PasswordHelper passwords;

    @Transactional(readOnly = true)
    public Page<User> search(String keyword, int page) {
        String text = ValidationUtil.trim(keyword).toLowerCase(Locale.ROOT);
        return users.searchStaff(Role.LIBRARIAN, "%" + text + "%", PageRequest.of(Math.max(page, 0), 10));
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).filter(u -> u.getRole() == Role.LIBRARIAN)
                .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
    }

    @Transactional
    public User create(StaffForm form) {
        passwords.validateNewPassword(form.getPassword(), form.getConfirmPassword());
        User user = new User();
        user.setRole(Role.LIBRARIAN);
        user.setStatus(AccountStatus.ACTIVE);
        user.setPasswordHash(passwords.hash(form.getPassword()));
        updateFields(user, form);
        try { return users.saveAndFlush(user); }
        catch (DataIntegrityViolationException e) { throw new BusinessException("Email này đã được đăng ký"); }
    }

    @Transactional
    public void update(Long id, StaffForm form) {
        User user = get(id);
        updateFields(user, form);
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException e) { throw new BusinessException("Email này đã được đăng ký"); }
    }

    @Transactional
    public void setLocked(Long id, boolean locked) {
        get(id).setStatus(locked ? AccountStatus.LOCKED : AccountStatus.ACTIVE);
    }

    private void updateFields(User user, StaffForm form) {
        String name = ValidationUtil.trim(form.getFullName());
        String email = ValidationUtil.normalizeEmail(form.getEmail());
        String phone = ValidationUtil.trim(form.getPhone());
        String address = ValidationUtil.trim(form.getAddress());
        ValidationUtil.checkFullName(name);
        if (!ValidationUtil.isValidEmail(email)) throw new BusinessException("Email không hợp lệ");
        ValidationUtil.checkPhoneIfPresent(phone);
        if (address.length() > 255) throw new BusinessException("Địa chỉ tối đa 255 ký tự");
        users.findByEmail(email).filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> { throw new BusinessException("Email này đã được đăng ký"); });
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setAddress(address.isEmpty() ? null : address);
    }
}
