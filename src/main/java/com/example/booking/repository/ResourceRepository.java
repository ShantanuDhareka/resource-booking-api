package com.example.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.booking.domain.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
