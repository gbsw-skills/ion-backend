package com.ion.admin.dto;

import java.time.Instant;

public record AdminLogResponse(
        Long id,
        String adminName,
        String action,
        String targetType,
        Long targetId,
        Instant createdAt
) {}
