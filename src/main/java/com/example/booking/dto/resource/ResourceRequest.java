package com.example.booking.dto.resource;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 120)
		String name,

		@Size(max = 1000)
		String description,

		@NotBlank(message = "Type is required")
		@Size(max = 64)
		String type,

		@Size(max = 255)
		String location,

		@NotNull(message = "Hourly rate is required")
		@DecimalMin(value = "0.00", message = "Hourly rate must be zero or positive")
		BigDecimal hourlyRate,

		Boolean available) {
}
