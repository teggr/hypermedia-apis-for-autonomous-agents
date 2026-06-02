package io.github.teggr.conventionalapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for creating a new order")
public record CreateOrderRequest(
        @NotBlank @Schema(description = "Customer identifier") String customerId,
        @NotBlank @Schema(description = "Description of the order") String description
) {}
