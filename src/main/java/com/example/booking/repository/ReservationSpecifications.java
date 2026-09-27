package com.example.booking.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.example.booking.domain.Reservation;
import com.example.booking.domain.ReservationStatus;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class ReservationSpecifications {

	private ReservationSpecifications() {
	}

	public static Specification<Reservation> withFilters(
			Long userId,
			ReservationStatus status,
			BigDecimal minPrice,
			BigDecimal maxPrice) {
		return (root, query, cb) -> {
			if (query != null && query.getResultType() != null && query.getResultType() != Long.class
					&& query.getResultType() != long.class) {
				root.fetch("resource", JoinType.LEFT);
				root.fetch("user", JoinType.LEFT);
				query.distinct(true);
			}
			Predicate predicate = cb.conjunction();
			if (userId != null) {
				predicate = cb.and(predicate, cb.equal(root.get("user").get("id"), userId));
			}
			if (status != null) {
				predicate = cb.and(predicate, cb.equal(root.get("status"), status));
			}
			if (minPrice != null) {
				predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
			}
			if (maxPrice != null) {
				predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
			}
			return predicate;
		};
	}
}
