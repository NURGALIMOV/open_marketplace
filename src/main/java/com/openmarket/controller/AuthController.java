package com.openmarket.controller;

import com.openmarket.dto.auth.LoginRequest;
import com.openmarket.dto.auth.LoginResponse;
import com.openmarket.entity.User;
import com.openmarket.exception.AppAuthException;
import com.openmarket.service.JwtService;
import com.openmarket.service.UserService;
import com.openmarket.utils.LogWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String X_REAL_IP_HEADER = "X-Real-IP";
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        var response = LogWrapper.logWrap(
                log,
                "login",
                () -> {
                    log.info("Login attempt for email: {}", request.getEmail());
                    return userService.authenticate(request.getEmail(), request.getPassword())
                            .map(this::buildLoginResponse)
                            .orElseThrow(() -> {
                                log.warn("Login failed for email: {} from IP: {}", request.getEmail(), getClientIpAddress(httpRequest));
                                return new AppAuthException("Invalid email or password");
                            });
                }
        );
        return ResponseEntity.ok(response);
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        long expirationTimeSeconds = jwtService.getExpirationTimeSeconds();
        LoginResponse result = LoginResponse.builder().accessToken(token).expiresIn(expirationTimeSeconds).build();
        log.info("Login successful for user: {} with role: {}", user.getEmail(), user.getRole());
        return result;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader(X_REAL_IP_HEADER);
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
