package fu.libmanagement.user.service;

import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.FileStorageService;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.user.dto.ProfileForm;
import fu.libmanagement.user.entity.ReaderCard;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.ReaderCardRepository;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** UC4 Cập nhật hồ sơ cá nhân của độc giả. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReaderCardRepository readerCardRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản"));
    }

    /** Thẻ thư viện của độc giả (null nếu tài khoản không có thẻ, ví dụ nhân viên). */
    @Transactional(readOnly = true)
    public ReaderCard getCard(Long userId) {
        return readerCardRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Sửa họ tên, SĐT, địa chỉ và ảnh đại diện (không chọn ảnh thì giữ ảnh cũ).
     * Không có chỗ nào sửa email hay mã thẻ.
     */
    @Transactional
    public User updateProfile(Long userId, ProfileForm form, MultipartFile avatar) {
        String fullName = ValidationUtil.trim(form.getFullName());
        String phone = ValidationUtil.trim(form.getPhone());
        String address = ValidationUtil.trim(form.getAddress());

        ValidationUtil.checkNameAndPhone(fullName, phone);
        if (address.length() > 255) {
            throw new BusinessException("Địa chỉ tối đa 255 ký tự");
        }

        User user = getUser(userId);
        String oldAvatar = null;
        if (avatar != null && !avatar.isEmpty()) {
            oldAvatar = user.getAvatarUrl();
            user.setAvatarUrl(fileStorageService.saveImage(avatar, "avatars"));
        }
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address.isEmpty() ? null : address);
        // user đang được quản lý trong transaction nên Hibernate tự UPDATE khi commit (dirty checking)

        fileStorageService.delete(oldAvatar);
        return user;
    }
}
