package com.example.booking.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.booking.domain.Reservation;
import com.example.booking.domain.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

	@Override
	@EntityGraph(attributePaths = { "resource", "user" })
	Optional<Reservation> findById(Long id);

	@Query("""
			SELECT r FROM Reservation r
			WHERE r.resource.id = :resourceId
			  AND r.status IN :activeStatuses
			  AND r.startTime < :endTime
			  AND r.endTime > :startTime
			  AND (:excludeId IS NULL OR r.id <> :excludeId)
			""")
	List<Reservation> findOverlapping(
			@Param("resourceId") Long resourceId,
			@Param("startTime") Instant startTime,
			@Param("endTime") Instant endTime,
			@Param("activeStatuses") List<ReservationStatus> activeStatuses,
			@Param("excludeId") Long excludeId);
}
