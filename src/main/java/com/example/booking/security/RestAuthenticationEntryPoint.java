package com.example.booking.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		writeJson(response, HttpStatus.UNAUTHORIZED, "Authentication is required", request.getRequestURI());
	}

	static void writeJson(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String body = """
				{"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s","details":[]}
				""".formatted(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				escape(message),
				escape(path));
		response.getWriter().write(body);
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
