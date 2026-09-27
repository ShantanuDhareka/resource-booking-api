package com.example.booking.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "Username is required")
		@Size(max = 64)
		String username,

		@NotBlank(message = "Password is required")
		@Size(min = 6, max = 100)
		String password) {
}
