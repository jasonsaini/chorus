package com.chorus.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoomRequest(@NotBlank String createdBy) {}
