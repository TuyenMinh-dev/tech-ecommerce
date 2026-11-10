package com.haui.auth.service;

import com.haui.auth.dto.AuthResponse;
import com.haui.auth.dto.LoginRequest;
import com.haui.auth.dto.RegisterRequest;
import com.haui.auth.model.User;
import com.haui.auth.repository.UserRepository;
import com.haui.auth.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + req.getUsername() + "' đã tồn tại!");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email '" + req.getEmail() + "' đã tồn tại!");
        }

        String role = (req.getRole() != null && !req.getRole().isBlank()) 
                ? req.getRole().toUpperCase() 
                : "ROLE_CUSTOMER";
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .fullName(req.getFullName())
                .role(role)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: {} (Role: {})", saved.getUsername(), saved.getRole());

        String token = jwtUtil.generateToken(saved.getId(), saved.getUsername(), saved.getRole());

        return AuthResponse.builder()
                .token(token)
                .id(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .role(saved.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác!"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public Map<String, Object> validateToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        boolean isValid = jwtUtil.validateToken(token);
        if (!isValid) {
            return Map.of("valid", false, "message", "Token không hợp lệ hoặc đã hết hạn!");
        }
        Claims claims = jwtUtil.getClaimsFromToken(token);
        return Map.of(
                "valid", true,
                "userId", claims.get("userId"),
                "username", claims.getSubject(),
                "role", claims.get("role")
        );
    }
}
