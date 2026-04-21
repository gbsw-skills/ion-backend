package com.ion.auth.service;

import com.ion.auth.dto.LoginRequest;
import com.ion.auth.dto.LoginResponse;
import com.ion.auth.dto.RefreshTokenRequest;
import com.ion.auth.dto.RefreshTokenResponse;
import com.ion.auth.jwt.RefreshToken;
import com.ion.auth.jwt.RefreshTokenRepository;
import com.ion.auth.jwt.JwtProvider;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.user.domain.User;
import com.ion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${ion.jwt.access-expiry}")
    private long accessExpirySeconds;

    @Value("${ion.jwt.refresh-expiry}")
    private long refreshExpirySeconds;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IonException(ErrorCode.AUTH_002));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IonException(ErrorCode.AUTH_002);
        }

        return issueLoginResponse(user);
    }

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        if (!jwtProvider.validateRefreshToken(request.refreshToken())) {
            throw new IonException(ErrorCode.AUTH_001);
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IonException(ErrorCode.AUTH_001));

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new IonException(ErrorCode.AUTH_001));

        refreshTokenRepository.delete(refreshToken);

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole().name());
        persistRefreshToken(user.getId(), newRefreshToken);

        return new RefreshTokenResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                accessExpirySeconds
        );
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    private LoginResponse issueLoginResponse(User user) {
        refreshTokenRepository.deleteAllByUserId(user.getId());

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole().name());
        persistRefreshToken(user.getId(), refreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken,
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

    private void persistRefreshToken(Long userId, String refreshToken) {
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .token(refreshToken)
                .expiresAt(Instant.now().plusSeconds(refreshExpirySeconds))
                .build());
    }
}
