package com.ion.user.dto;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String username,
        String displayName,
        String role,
        Instant createdAt
) {
}
