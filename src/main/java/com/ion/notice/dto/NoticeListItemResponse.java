package com.ion.notice.dto;

import java.time.Instant;

public record NoticeListItemResponse(
        Long id,
        String title,
        Instant publishedAt
) {}
