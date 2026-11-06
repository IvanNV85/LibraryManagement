package fu.libmanagement.common.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Bắt BusinessException mà controller không tự bắt, hiện trang thông báo lỗi thân thiện
 * (thay vì trang lỗi 500). Controller vẫn nên tự bắt BusinessException khi muốn giữ lại form đang nhập.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error/business";
    }
}
