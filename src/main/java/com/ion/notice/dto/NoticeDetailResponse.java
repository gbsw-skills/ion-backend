package com.ion.notice.dto;

import java.time.Instant;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        String authorName,
        Instant publishedAt,
        Instant createdAt
) {}
