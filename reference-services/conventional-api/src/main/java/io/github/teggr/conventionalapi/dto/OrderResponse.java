package io.github.teggr.conventionalapi.dto;

import io.github.teggr.conventionalapi.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Representation of an order")
public record OrderResponse(
        @Schema(description = "Unique order identifier") Long id,
        @Schema(description = "Customer who placed the order") String customerId,
        @Schema(description = "Human-readable order description") String description,
        @Schema(description = "Current lifecycle status") OrderStatus status,
        @Schema(description = "ISO-8601 creation timestamp") Instant createdAt,
        @Schema(description = "ISO-8601 last-update timestamp") Instant updatedAt
) {}
