package com.example.booking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.example.booking.domain.Role;
import com.example.booking.exception.ForbiddenException;

@Component
public class CurrentUser {

	public Long requireUserId() {
		Jwt jwt = requireJwt();
		Object claim = jwt.getClaim("userId");
		if (claim instanceof Number number) {
			return number.longValue();
		}
		throw new ForbiddenException("JWT is missing a valid userId claim");
	}

	public String requireUsername() {
		return requireJwt().getSubject();
	}

	public boolean isAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(authority -> authority.equals("ROLE_" + Role.ADMIN.name()));
	}

	private Jwt requireJwt() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new ForbiddenException("Authenticated JWT principal is required");
		}
		return jwt;
	}
}
