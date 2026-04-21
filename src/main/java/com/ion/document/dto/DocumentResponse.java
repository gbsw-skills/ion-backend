package com.ion.document.dto;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String title,
        String fileType,
        Instant uploadedAt
) {}
