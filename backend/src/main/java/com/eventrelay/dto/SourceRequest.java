package com.eventrelay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SourceRequest(
    @NotBlank @Size(max = 64) String name,
    @NotBlank @Size(max = 128) String displayName,
    @NotBlank String signingSecret
) {
}
