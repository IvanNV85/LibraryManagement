package fu.libmanagement.common.util;

import fu.libmanagement.common.exception.BusinessException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Lưu ảnh upload (ảnh đại diện, ảnh bìa sách) vào thư mục trên ổ đĩa.
 * File được phục vụ lại qua đường dẫn /uploads/** (xem WebConfig).
 */
@Slf4j
@Service
public class FileStorageService {

    private static final long MAX_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String URL_PREFIX = "/uploads/";

    @Getter
    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /**
     * Lưu ảnh JPG/PNG vào thư mục con {@code folder}, trả về URL để lưu vào DB (vd /uploads/avatars/abc.png).
     * Tên file do server tự sinh, không dùng tên file người dùng gửi lên.
     */
    public String saveImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Chưa chọn file ảnh");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("Ảnh tối đa 2MB");
        }
        String extension;
        if ("image/jpeg".equals(file.getContentType())) {
            extension = ".jpg";
        } else if ("image/png".equals(file.getContentType())) {
            extension = ".png";
        } else {
            throw new BusinessException("Chỉ chấp nhận ảnh JPG hoặc PNG");
        }

        String fileName = UUID.randomUUID() + extension;
        try {
            Path dir = root.resolve(folder);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            log.error("Lỗi lưu file ảnh", e);
            throw new BusinessException("Không lưu được ảnh, vui lòng thử lại");
        }
        return URL_PREFIX + folder + "/" + fileName;
    }

    /** Xóa file theo URL đã lưu trong DB (bỏ qua nếu URL rỗng hoặc không thuộc thư mục upload). */
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        try {
            Path file = root.resolve(url.substring(URL_PREFIX.length())).normalize();
            if (file.startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Không xóa được file {}: {}", url, e.getMessage());
        }
    }
}
