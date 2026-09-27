package com.example.booking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.booking.domain.Reservation;
import com.example.booking.domain.ReservationStatus;
import com.example.booking.domain.Resource;
import com.example.booking.domain.User;
import com.example.booking.dto.common.PageResponse;
import com.example.booking.dto.reservation.ReservationCreateRequest;
import com.example.booking.dto.reservation.ReservationResponse;
import com.example.booking.dto.reservation.ReservationUpdateRequest;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ReservationSpecifications;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.CurrentUser;

@Service
@Transactional
public class ReservationService {

	private static final List<ReservationStatus> ACTIVE_STATUSES = List.of(
			ReservationStatus.PENDING,
			ReservationStatus.CONFIRMED);

	private static final Set<String> SORTABLE_FIELDS = Set.of(
			"id", "price", "status", "startTime", "endTime", "createdAt", "updatedAt");

	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final ResourceService resourceService;
	private final CurrentUser currentUser;

	public ReservationService(
			ReservationRepository reservationRepository,
			UserRepository userRepository,
			ResourceService resourceService,
			CurrentUser currentUser) {
		this.reservationRepository = reservationRepository;
		this.userRepository = userRepository;
		this.resourceService = resourceService;
		this.currentUser = currentUser;
	}

	@Transactional(readOnly = true)
	public PageResponse<ReservationResponse> search(
			ReservationStatus status,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			int page,
			int size,
			String sort) {
		if (page < 0) {
			throw new BadRequestException("page must be zero or greater");
		}
		if (size < 1 || size > 100) {
			throw new BadRequestException("size must be between 1 and 100");
		}
		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new BadRequestException("minPrice cannot be greater than maxPrice");
		}

		Long ownerFilter = currentUser.isAdmin() ? null : currentUser.requireUserId();
		Pageable pageable = PageRequest.of(page, size, parseSort(sort));
		Page<ReservationResponse> result = reservationRepository
				.findAll(ReservationSpecifications.withFilters(ownerFilter, status, minPrice, maxPrice), pageable)
				.map(ReservationResponse::from);
		return PageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public ReservationResponse findById(Long id) {
		Reservation reservation = getReservation(id);
		assertCanView(reservation);
		return ReservationResponse.from(reservation);
	}

	public ReservationResponse create(ReservationCreateRequest request) {
		validateWindow(request.startTime(), request.endTime());
		Resource resource = resourceService.getResource(request.resourceId());
		if (!resource.isAvailable()) {
			throw new ConflictException("Resource is not available for booking");
		}
		assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), null);

		User owner = userRepository.findById(currentUser.requireUserId())
				.orElseThrow(() -> new NotFoundException("Authenticated user was not found"));

		Reservation reservation = new Reservation();
		reservation.setResource(resource);
		reservation.setUser(owner);
		reservation.setStartTime(request.startTime());
		reservation.setEndTime(request.endTime());
		reservation.setStatus(ReservationStatus.PENDING);
		reservation.setPrice(calculatePrice(resource.getHourlyRate(), request.startTime(), request.endTime()));
		reservation.setNotes(request.notes());
		return ReservationResponse.from(reservationRepository.save(reservation));
	}

	public ReservationResponse update(Long id, ReservationUpdateRequest request) {
		Reservation reservation = getReservation(id);
		if (currentUser.isAdmin()) {
			return updateAsAdmin(reservation, request);
		}
		return updateAsUser(reservation, request);
	}

	public void delete(Long id) {
		if (!currentUser.isAdmin()) {
			throw new ForbiddenException("Only administrators can delete reservations");
		}
		Reservation reservation = getReservation(id);
		reservationRepository.delete(reservation);
	}

	private ReservationResponse updateAsAdmin(Reservation reservation, ReservationUpdateRequest request) {
		Instant start = request.startTime() != null ? request.startTime() : reservation.getStartTime();
		Instant end = request.endTime() != null ? request.endTime() : reservation.getEndTime();
		validateWindow(start, end);

		Resource resource = reservation.getResource();
		if (request.resourceId() != null && !request.resourceId().equals(resource.getId())) {
			resource = resourceService.getResource(request.resourceId());
		}

		ReservationStatus status = request.status() != null ? request.status() : reservation.getStatus();
		if (status != ReservationStatus.CANCELLED) {
			assertNoOverlap(resource.getId(), start, end, reservation.getId());
		}

		reservation.setResource(resource);
		reservation.setStartTime(start);
		reservation.setEndTime(end);
		reservation.setStatus(status);
		if (request.price() != null) {
			reservation.setPrice(request.price().setScale(2, RoundingMode.HALF_UP));
		} else if (request.startTime() != null || request.endTime() != null || request.resourceId() != null) {
			reservation.setPrice(calculatePrice(resource.getHourlyRate(), start, end));
		}
		if (request.notes() != null) {
			reservation.setNotes(request.notes());
		}
		return ReservationResponse.from(reservationRepository.save(reservation));
	}

	private ReservationResponse updateAsUser(Reservation reservation, ReservationUpdateRequest request) {
		assertOwner(reservation);
		if (reservation.getStatus() == ReservationStatus.CANCELLED) {
			throw new ConflictException("Cancelled reservations cannot be updated");
		}
		if (request.resourceId() != null && !request.resourceId().equals(reservation.getResource().getId())) {
			throw new ForbiddenException("Users cannot reassign a reservation to another resource");
		}
		if (request.price() != null) {
			throw new ForbiddenException("Users cannot override reservation price");
		}
		if (request.status() != null && request.status() != ReservationStatus.CANCELLED) {
			throw new ForbiddenException("Users can only cancel their own reservations");
		}

		if (request.status() == ReservationStatus.CANCELLED) {
			reservation.setStatus(ReservationStatus.CANCELLED);
			if (request.notes() != null) {
				reservation.setNotes(request.notes());
			}
			return ReservationResponse.from(reservationRepository.save(reservation));
		}

		Instant start = request.startTime() != null ? request.startTime() : reservation.getStartTime();
		Instant end = request.endTime() != null ? request.endTime() : reservation.getEndTime();
		validateWindow(start, end);
		assertNoOverlap(reservation.getResource().getId(), start, end, reservation.getId());
		reservation.setStartTime(start);
		reservation.setEndTime(end);
		reservation.setPrice(calculatePrice(reservation.getResource().getHourlyRate(), start, end));
		if (request.notes() != null) {
			reservation.setNotes(request.notes());
		}
		return ReservationResponse.from(reservationRepository.save(reservation));
	}

	private Reservation getReservation(Long id) {
		return reservationRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
	}

	private void assertCanView(Reservation reservation) {
		if (currentUser.isAdmin()) {
			return;
		}
		assertOwner(reservation);
	}

	private void assertOwner(Reservation reservation) {
		if (!reservation.getUser().getId().equals(currentUser.requireUserId())) {
			throw new ForbiddenException("You can only access your own reservations");
		}
	}

	private void validateWindow(Instant start, Instant end) {
		if (start == null || end == null) {
			throw new BadRequestException("Start time and end time are required");
		}
		if (!end.isAfter(start)) {
			throw new BadRequestException("End time must be after start time");
		}
	}

	private void assertNoOverlap(Long resourceId, Instant start, Instant end, Long excludeId) {
		List<Reservation> overlaps = reservationRepository.findOverlapping(
				resourceId, start, end, ACTIVE_STATUSES, excludeId);
		if (!overlaps.isEmpty()) {
			throw new ConflictException("The resource is already booked for the selected time window");
		}
	}

	private BigDecimal calculatePrice(BigDecimal hourlyRate, Instant start, Instant end) {
		long minutes = Duration.between(start, end).toMinutes();
		if (minutes <= 0) {
			throw new BadRequestException("Reservation duration must be positive");
		}
		BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
		return hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
	}

	private Sort parseSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}
		String[] parts = sort.split(",");
		String field = parts[0].trim();
		if (!SORTABLE_FIELDS.contains(field)) {
			throw new BadRequestException("Unsupported sort field: " + field);
		}
		Sort.Direction direction = Sort.Direction.ASC;
		if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
			direction = Sort.Direction.DESC;
		}
		return Sort.by(direction, field);
	}
}
