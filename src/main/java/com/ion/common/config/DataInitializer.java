package com.ion.common.config;

import com.ion.user.domain.User;
import com.ion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin1234"))
                    .role(User.Role.ADMIN)
                    .displayName("관리자")
                    .build());
            log.info("Created default admin user (admin / admin1234)");
        }

        if (!userRepository.existsByUsername("student01")) {
            userRepository.save(User.builder()
                    .username("student01")
                    .passwordHash(passwordEncoder.encode("student1234"))
                    .role(User.Role.STUDENT)
                    .displayName("테스트 학생")
                    .build());
            log.info("Created default student user (student01 / student1234)");
        }
    }
}
