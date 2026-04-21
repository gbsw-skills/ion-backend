package com.ion.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record NoticeUpsertRequest(
        @NotBlank(message = "공지사항 제목은 필수입니다.")
        String title,
        @NotBlank(message = "공지사항 본문은 필수입니다.")
        String content,
        @NotNull(message = "게시 시각은 필수입니다.")
        Instant publishedAt
) {}
