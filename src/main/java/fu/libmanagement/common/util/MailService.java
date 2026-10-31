package fu.libmanagement.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Gửi email dùng chung (xác thực email, quên mật khẩu, thông báo UC15...).
 * Chạy bất đồng bộ để request của người dùng không phải chờ SMTP.
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String baseUrl;

    public MailService(JavaMailSender mailSender,
                       @Value("${app.mail.from}") String from,
                       @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    /**
     * Gửi email có liên kết dạng {baseUrl}{path}?token={token}.
     * Nội dung = textBefore, liên kết, textAfter (dùng cho xác thực email, đặt lại mật khẩu).
     */
    @Async
    public void sendLink(String to, String subject, String textBefore, String path, String token, String textAfter) {
        String link = baseUrl + path + "?token=" + token;
        deliver(to, subject, textBefore + "\n" + link + "\n\n" + textAfter);
    }

    @Async
    public void send(String to, String subject, String content) {
        deliver(to, subject, content);
    }

    // Không đánh @Async ở đây: gọi nội bộ trong cùng class thì @Async không có tác dụng
    private void deliver(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (MailException e) {
            // Khi chưa cấu hình SMTP (máy dev) thì in nội dung ra log để vẫn lấy được liên kết
            log.warn("Không gửi được email tới {}: {}", to, e.getMessage());
            log.warn("Nội dung email:\n{}", content);
        }
    }
}
