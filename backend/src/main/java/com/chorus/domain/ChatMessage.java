package com.chorus.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(MessageType type, String sender, String content, Instant timestamp) {

    public static ChatMessage of(MessageType type, String sender, String content) {
        return new ChatMessage(type, sender, content, Instant.now());
    }
}
