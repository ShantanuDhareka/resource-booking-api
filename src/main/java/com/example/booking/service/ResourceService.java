package com.example.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.booking.domain.Resource;
import com.example.booking.dto.resource.ResourceRequest;
import com.example.booking.dto.resource.ResourceResponse;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.ResourceRepository;

@Service
@Transactional
public class ResourceService {

	private final ResourceRepository resourceRepository;

	public ResourceService(ResourceRepository resourceRepository) {
		this.resourceRepository = resourceRepository;
	}

	@Transactional(readOnly = true)
	public List<ResourceResponse> findAll() {
		return resourceRepository.findAll().stream()
				.map(ResourceResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ResourceResponse findById(Long id) {
		return ResourceResponse.from(getResource(id));
	}

	public ResourceResponse create(ResourceRequest request) {
		Resource resource = new Resource();
		apply(resource, request);
		return ResourceResponse.from(resourceRepository.save(resource));
	}

	public ResourceResponse update(Long id, ResourceRequest request) {
		Resource resource = getResource(id);
		apply(resource, request);
		return ResourceResponse.from(resourceRepository.save(resource));
	}

	public void delete(Long id) {
		Resource resource = getResource(id);
		resourceRepository.delete(resource);
	}

	public Resource getResource(Long id) {
		return resourceRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Resource not found: " + id));
	}

	private void apply(Resource resource, ResourceRequest request) {
		resource.setName(request.name());
		resource.setDescription(request.description());
		resource.setType(request.type());
		resource.setLocation(request.location());
		resource.setHourlyRate(request.hourlyRate());
		resource.setAvailable(request.available() == null || request.available());
	}
}
