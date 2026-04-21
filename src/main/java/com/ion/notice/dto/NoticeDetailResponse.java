package com.ion.notice.dto;

import java.time.Instant;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        Instant publishedAt,
        Instant createdAt
) {}
