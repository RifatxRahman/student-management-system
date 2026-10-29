package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AdminUserDtos.CreatedUserResponse;
import com.sms.studentmanagement.dto.UserResponse;
import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.RefreshTokenRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll(Sort.by("username")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public CreatedUserResponse create(String username, String fullName, Role role) {
        String normalized = username.trim();
        if (userRepository.existsByUsername(normalized)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই ইউজারনেম আগে থেকেই আছে");
        }
        String temporaryPassword = TemporaryPasswordGenerator.generate();

        User user = new User();
        user.setUsername(normalized);
        user.setFullName(fullName.trim());
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        userRepository.saveAndFlush(user);

        return new CreatedUserResponse(UserResponse.from(user), temporaryPassword);
    }

    @Transactional
    public String resetPassword(Long targetId) {
        User user = find(targetId);
        String temporaryPassword = TemporaryPasswordGenerator.generate();

        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        // পরিবর্তনগুলো আগে সেভ হবে, তারপর সব পুরনো লগইন বাতিল
        refreshTokenRepository.revokeAllActive(user.getId(), Instant.now());
        return temporaryPassword;
    }

    @Transactional
    public UserResponse deactivate(Long targetId, Long actingAdminId) {
        User user = find(targetId);
        if (user.getId().equals(actingAdminId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "নিজের অ্যাকাউন্ট নিষ্ক্রিয় করা যাবে না");
        }
        if (user.getRole() == Role.ADMIN
                && user.getStatus() == UserStatus.ACTIVE
                && userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শেষ সক্রিয় Admin নিষ্ক্রিয় করা যাবে না");
        }
        user.setStatus(UserStatus.DEACTIVATED);
        refreshTokenRepository.revokeAllActive(user.getId(), Instant.now());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse activate(Long targetId) {
        User user = find(targetId);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return UserResponse.from(user);
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ব্যবহারকারী পাওয়া যায়নি"));
    }
}