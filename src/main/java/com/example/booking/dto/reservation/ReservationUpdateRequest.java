package com.example.booking.dto.reservation;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.booking.domain.ReservationStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record ReservationUpdateRequest(
		Long resourceId,
		Instant startTime,
		Instant endTime,
		ReservationStatus status,
		@DecimalMin(value = "0.00", message = "Price must be zero or positive")
		BigDecimal price,
		@Size(max = 1000)
		String notes) {
}
