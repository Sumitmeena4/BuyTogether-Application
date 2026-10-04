package com.hackathon.gemma_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyzeRequest(
        @NotBlank @Size(max = 20000) String conversation) {
}
