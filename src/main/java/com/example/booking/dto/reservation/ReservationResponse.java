package com.example.booking.dto.reservation;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.booking.domain.Reservation;
import com.example.booking.domain.ReservationStatus;

public record ReservationResponse(
		Long id,
		Long resourceId,
		String resourceName,
		Long userId,
		String username,
		Instant startTime,
		Instant endTime,
		ReservationStatus status,
		BigDecimal price,
		String notes,
		Instant createdAt,
		Instant updatedAt) {

	public static ReservationResponse from(Reservation reservation) {
		return new ReservationResponse(
				reservation.getId(),
				reservation.getResource().getId(),
				reservation.getResource().getName(),
				reservation.getUser().getId(),
				reservation.getUser().getUsername(),
				reservation.getStartTime(),
				reservation.getEndTime(),
				reservation.getStatus(),
				reservation.getPrice(),
				reservation.getNotes(),
				reservation.getCreatedAt(),
				reservation.getUpdatedAt());
	}
}
