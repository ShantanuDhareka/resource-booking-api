package com.example.booking.dto.resource;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.booking.domain.Resource;

public record ResourceResponse(
		Long id,
		String name,
		String description,
		String type,
		String location,
		BigDecimal hourlyRate,
		boolean available,
		Instant createdAt,
		Instant updatedAt) {

	public static ResourceResponse from(Resource resource) {
		return new ResourceResponse(
				resource.getId(),
				resource.getName(),
				resource.getDescription(),
				resource.getType(),
				resource.getLocation(),
				resource.getHourlyRate(),
				resource.isAvailable(),
				resource.getCreatedAt(),
				resource.getUpdatedAt());
	}
}
