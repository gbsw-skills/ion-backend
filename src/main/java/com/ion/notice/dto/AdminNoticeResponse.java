package com.ion.notice.dto;

import java.time.Instant;

public record AdminNoticeResponse(
        Long id,
        String title,
        Instant publishedAt,
        Instant createdAt
) {}
