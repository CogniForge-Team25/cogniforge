package com.cogniforge.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Creates the first administrator when the users table is empty.
// After the first successful start, remove BOOTSTRAP_ADMIN_* from the environment.
@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements ApplicationRunner {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || users.count() > 0) {
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
        }
        User admin = new User();
        admin.setEmail(email.trim().toLowerCase());
        admin.setFullName("System Administrator");
        admin.setPasswordHash(encoder.encode(password));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRole(roles.findByName("IT_ADMINISTRATOR").orElseThrow());
        users.save(admin);
    }
}
