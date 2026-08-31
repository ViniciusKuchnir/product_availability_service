package com.example.product_availability_service.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(

        @Schema(
                description = "Timestamp when the error occurred",
                example = "2026-08-30T20:00:00Z"
        )
        Instant timestamp,

        @Schema(example = "404")
        int status,

        @Schema(example = "PRODUCT_NOT_FOUND")
        String code,

        @Schema(
                example = "Product with SKU MON-34 was not found"
        )
        String message
) {
}
