package com.ion.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(max = 50, message = "아이디는 50자 이하여야 합니다.")
        String username,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,
        @NotBlank(message = "사용자 역할은 필수입니다.")
        String role,
        @NotBlank(message = "표시 이름은 필수입니다.")
        @Size(max = 100, message = "표시 이름은 100자 이하여야 합니다.")
        String displayName
) {
}
