package com.ion.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
        String id,
        String object,
        List<Choice> choices
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(int index, Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {}
}
