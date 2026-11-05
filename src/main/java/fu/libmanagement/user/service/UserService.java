package fu.libmanagement.user.service;

import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.user.dto.ProfileForm;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC4 Xem và cập nhật hồ sơ cá nhân. Các module khác dùng {@link #getUser} để lấy thông tin tài khoản. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản"));
    }

    /**
     * Sửa họ tên, SĐT (có thể để trống) và địa chỉ.
     * Không có chỗ nào sửa email, mã thẻ hay vai trò.
     */
    @Transactional
    public User updateProfile(Long userId, ProfileForm form) {
        String fullName = ValidationUtil.trim(form.getFullName());
        String phone = ValidationUtil.trim(form.getPhone());
        String address = ValidationUtil.trim(form.getAddress());

        ValidationUtil.checkFullName(fullName);
        ValidationUtil.checkPhoneIfPresent(phone);
        if (address.length() > 255) {
            throw new BusinessException("Địa chỉ tối đa 255 ký tự");
        }

        User user = getUser(userId);
        user.setFullName(fullName);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setAddress(address.isEmpty() ? null : address);
        // user đang được quản lý trong transaction nên Hibernate tự UPDATE khi commit (dirty checking)
        return user;
    }
}
