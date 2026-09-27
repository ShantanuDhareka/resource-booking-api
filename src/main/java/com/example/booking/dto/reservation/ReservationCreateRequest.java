package com.example.booking.dto.reservation;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record ReservationCreateRequest(
		@NotNull(message = "Resource ID is required")
		Long resourceId,

		@NotNull(message = "Start time is required")
		Instant startTime,

		@NotNull(message = "End time is required")
		Instant endTime,

		String notes) {
}
