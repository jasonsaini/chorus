package com.chorus.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LinkRepoRequest(@NotBlank String repoFullName, String branch) {}
