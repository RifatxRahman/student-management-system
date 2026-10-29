package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AdminUserDtos.CreateUserRequest;
import com.sms.studentmanagement.dto.AdminUserDtos.CreatedUserResponse;
import com.sms.studentmanagement.dto.AdminUserDtos.TemporaryPasswordResponse;
import com.sms.studentmanagement.dto.UserResponse;
import com.sms.studentmanagement.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public List<UserResponse> list() {
        return adminUserService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedUserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return adminUserService.create(request.username(), request.fullName(), request.role());
    }

    @PostMapping("/{id}/reset-password")
    public TemporaryPasswordResponse resetPassword(@PathVariable Long id) {
        return new TemporaryPasswordResponse(adminUserService.resetPassword(id));
    }

    @PostMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return adminUserService.deactivate(id, Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id) {
        return adminUserService.activate(id);
    }
}