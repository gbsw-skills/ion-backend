package com.ion.user.service;

import com.ion.admin.service.AdminLogService;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.user.domain.User;
import com.ion.user.dto.AdminUserCreateRequest;
import com.ion.user.dto.AdminUserResponse;
import com.ion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminLogService adminLogService;

    @Transactional
    public AdminUserResponse createUser(Long adminId, AdminUserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IonException(ErrorCode.USER_001);
        }

        User.Role role = parseRole(request.role());
        User user = userRepository.save(User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .displayName(request.displayName())
                .build());

        adminLogService.log(adminId, "CREATE_USER", "user", user.getId());
        return toResponse(user);
    }

    private User.Role parseRole(String role) {
        try {
            return User.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IonException(ErrorCode.USER_002);
        }
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
