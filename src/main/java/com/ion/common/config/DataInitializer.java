package com.ion.common.config;

import com.ion.user.domain.User;
import com.ion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${ion.bootstrap.user.username:}")
    private String bootstrapUsername;

    @Value("${ion.bootstrap.user.password:}")
    private String bootstrapPassword;

    @Value("${ion.bootstrap.user.role:ADMIN}")
    private String bootstrapRole;

    @Value("${ion.bootstrap.user.display-name:}")
    private String bootstrapDisplayName;

    @Override
    public void run(ApplicationArguments args) {
        if (isLocalProfile()) {
            createUserIfAbsent("admin", "admin1234", User.Role.ADMIN, "관리자");
            createUserIfAbsent("student01", "student1234", User.Role.STUDENT, "테스트 학생");
        }

        createBootstrapUserIfConfigured();
    }

    private boolean isLocalProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("local");
    }

    private void createBootstrapUserIfConfigured() {
        if (!StringUtils.hasText(bootstrapUsername) && !StringUtils.hasText(bootstrapPassword)) {
            return;
        }

        if (!StringUtils.hasText(bootstrapUsername) || !StringUtils.hasText(bootstrapPassword)) {
            log.warn("Skipped bootstrap user creation because username or password is missing");
            return;
        }

        User.Role role = User.Role.valueOf(bootstrapRole.toUpperCase());
        String displayName = StringUtils.hasText(bootstrapDisplayName) ? bootstrapDisplayName : bootstrapUsername;
        createUserIfAbsent(bootstrapUsername, bootstrapPassword, role, displayName);
    }

    private void createUserIfAbsent(String username, String password, User.Role role, String displayName) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .displayName(displayName)
                .build());
        log.info("Created bootstrap user '{}' with role '{}'", username, role);
    }
}
