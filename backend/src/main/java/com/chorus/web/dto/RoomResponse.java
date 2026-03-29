package com.chorus.web.dto;

import java.time.Instant;

public record RoomResponse(String roomId, String inviteCode, String createdBy, Instant createdAt) {}
