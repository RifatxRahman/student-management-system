package com.sms.studentmanagement.config;

import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserStatus;
import com.sms.studentmanagement.repository.UserRepository;
import com.sms.studentmanagement.service.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        String username = props.bootstrap().adminUsername();
        String password = props.bootstrap().adminPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("কোনো ব্যবহারকারী নেই এবং ADMIN_USERNAME/ADMIN_PASSWORD সেট করা নেই। প্রথম Admin তৈরি হয়নি।");
            return;
        }
        PasswordPolicy.validate(password, username);

        User admin = new User();
        admin.setUsername(username.trim());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName("System Admin");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        log.info("প্রথম Admin তৈরি হয়েছে: {}", admin.getUsername());
    }
}