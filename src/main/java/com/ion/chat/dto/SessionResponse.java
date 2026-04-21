package com.ion.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID sessionId,
        String title,
        Instant createdAt,
        Instant lastActiveAt
) {}
