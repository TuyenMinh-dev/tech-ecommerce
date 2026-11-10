package com.haui.auth.config;

import com.haui.auth.model.User;
import com.haui.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Bảng users trống, tự động nạp tài khoản mẫu cho Hệ thống Sàn Đồ Công Nghệ...");

            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@techecommerce.com")
                    .fullName("Quản trị viên Hệ thống")
                    .role("ROLE_ADMIN")
                    .build();

            User customer = User.builder()
                    .username("tuyenlm")
                    .password(passwordEncoder.encode("password123"))
                    .email("tuyenlm@haui.edu.vn")
                    .fullName("Lê Minh Tuyên")
                    .role("ROLE_CUSTOMER")
                    .build();

            userRepository.saveAll(List.of(admin, customer));
            log.info("Nạp tài khoản mẫu thành công: 'admin' (pass: admin123) và 'tuyenlm' (pass: password123)");
        }
    }
}
