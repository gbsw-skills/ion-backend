package com.ion.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        Long id,
        UUID sessionId,
        String role,
        String content,
        Instant createdAt
) {}
