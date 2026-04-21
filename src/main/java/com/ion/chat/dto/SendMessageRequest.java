package com.ion.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "메시지 내용이 비어 있습니다.")
        @Size(max = 2000, message = "메시지는 2000자를 초과할 수 없습니다.")
        String content
) {}
