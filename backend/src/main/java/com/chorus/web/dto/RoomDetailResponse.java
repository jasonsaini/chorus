package com.chorus.web.dto;

import com.chorus.domain.ChatMessage;
import java.util.List;

public record RoomDetailResponse(
        String roomId,
        List<String> participants,
        List<ChatMessage> messages,
        String linkedRepo,
        String linkedBranch) {}
