package fu.libmanagement.common.config;

import fu.libmanagement.auth.interceptor.RoleInterceptor;
import fu.libmanagement.common.enums.Role;
import fu.libmanagement.common.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Quy ước phân quyền theo tiền tố URL (mọi module cần theo):
 *   /account/**   cần đăng nhập (mọi vai trò)
 *   /reader/**    chỉ độc giả
 *   /librarian/** thủ thư hoặc admin
 *   /admin/**     chỉ admin
 * Các URL khác (/books, /login, /register...) là công khai.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService fileStorageService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleInterceptor()).addPathPatterns("/account/**");
        registry.addInterceptor(new RoleInterceptor(Role.READER)).addPathPatterns("/reader/**");
        registry.addInterceptor(new RoleInterceptor(Role.LIBRARIAN, Role.ADMIN)).addPathPatterns("/librarian/**");
        registry.addInterceptor(new RoleInterceptor(Role.ADMIN)).addPathPatterns("/admin/**");
    }

    /** Cho phép xem ảnh đã upload (ảnh đại diện, ảnh bìa) qua /uploads/**. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(fileStorageService.getRoot().toUri().toString());
    }
}
