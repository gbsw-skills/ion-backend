package com.ion.auth.service;

import com.ion.auth.dto.LoginRequest;
import com.ion.auth.dto.LoginResponse;
import com.ion.auth.jwt.JwtProvider;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.user.domain.User;
import com.ion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${ion.jwt.access-expiry}")
    private long accessExpirySeconds;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IonException(ErrorCode.AUTH_002));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IonException(ErrorCode.AUTH_002);
        }

        String token = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());

        return new LoginResponse(
                token,
                "Bearer",
                accessExpirySeconds,
                new LoginResponse.UserInfo(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getRole().name()
                )
        );
    }
}
