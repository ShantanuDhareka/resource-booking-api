package com.example.booking.web;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.booking.domain.ReservationStatus;
import com.example.booking.dto.common.PageResponse;
import com.example.booking.dto.reservation.ReservationCreateRequest;
import com.example.booking.dto.reservation.ReservationResponse;
import com.example.booking.dto.reservation.ReservationUpdateRequest;
import com.example.booking.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@GetMapping
	@Operation(summary = "List reservations with filters, pagination, and sorting")
	public PageResponse<ReservationResponse> list(
			@RequestParam(required = false) ReservationStatus status,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String sort) {
		return reservationService.search(status, minPrice, maxPrice, page, size, sort);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a reservation by ID")
	public ReservationResponse get(@PathVariable Long id) {
		return reservationService.findById(id);
	}

	@PostMapping
	@Operation(summary = "Create a reservation. The owner is taken from the JWT, not the request body.")
	public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a reservation. Users may reschedule or cancel their own bookings.")
	public ReservationResponse update(@PathVariable Long id, @Valid @RequestBody ReservationUpdateRequest request) {
		return reservationService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Delete a reservation (ADMIN)")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		reservationService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
