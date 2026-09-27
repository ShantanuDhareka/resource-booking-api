package com.example.booking.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.booking.domain.User;
import com.example.booking.dto.auth.LoginRequest;
import com.example.booking.dto.auth.LoginResponse;
import com.example.booking.security.JwtTokenService;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;

	public AuthService(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
	}

	public LoginResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		User user = (User) authentication.getPrincipal();
		String token = jwtTokenService.generateToken(user);
		return new LoginResponse(
				token,
				"Bearer",
				jwtTokenService.getExpirationSeconds(),
				user.getId(),
				user.getUsername(),
				user.getRole());
	}
}
