package com.adconnect.auth.config;

import com.adconnect.auth.entity.User;
import com.adconnect.auth.entity.UserStatus;
import com.adconnect.auth.repository.UserRepository;
import com.adconnect.common.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Creates the bootstrap admin and site-manager accounts on first start. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthProperties.Seed seed = properties.seed();
        ensure(seed.adminEmail(), seed.adminPassword(), "Platform Admin", Role.ADMIN);
        ensure(seed.managerEmail(), seed.managerPassword(), "Site Manager", Role.MANAGER);
    }

    private void ensure(String email, String password, String name, Role role) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(name);
        user.setRole(role);
        user.setStatus(UserStatus.VERIFIED);
        user.setVerifiedAt(Instant.now());
        users.save(user);
        log.info("Seeded {} account {}", role, email);
    }
}
