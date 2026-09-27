package com.example.booking.dto.auth;

import com.example.booking.domain.Role;

public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		Long userId,
		String username,
		Role role) {
}
