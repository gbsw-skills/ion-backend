package com.ion.admin.controller;

import com.ion.common.response.ApiResponse;
import com.ion.user.dto.AdminUserCreateRequest;
import com.ion.user.dto.AdminUserResponse;
import com.ion.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponse>> create(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody AdminUserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userAdminService.createUser(adminId, request)));
    }
}
