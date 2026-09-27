package com.example.booking.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.booking.domain.Resource;
import com.example.booking.domain.Role;
import com.example.booking.domain.User;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final UserRepository userRepository;
	private final ResourceRepository resourceRepository;
	private final PasswordEncoder passwordEncoder;

	public DataSeeder(
			UserRepository userRepository,
			ResourceRepository resourceRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.resourceRepository = resourceRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		seedUser("admin", "Admin@123", Role.ADMIN);
		seedUser("user", "User@123", Role.USER);
		if (resourceRepository.count() == 0) {
			resourceRepository.save(resource("Conference Room A", "ROOM", "Floor 2", "12-person meeting room", "75.00"));
			resourceRepository.save(resource("Delivery Van", "VEHICLE", "Garage B", "Cargo van, 3 seats", "40.00"));
			resourceRepository.save(resource("Projector Kit", "EQUIPMENT", "AV Closet", "4K projector and screen", "15.50"));
			log.info("Seeded sample resources");
		}
	}

	private void seedUser(String username, String rawPassword, Role role) {
		if (userRepository.existsByUsername(username)) {
			return;
		}
		userRepository.save(new User(username, passwordEncoder.encode(rawPassword), role));
		log.info("Seeded {} user '{}'", role, username);
	}

	private Resource resource(String name, String type, String location, String description, String hourlyRate) {
		Resource resource = new Resource();
		resource.setName(name);
		resource.setType(type);
		resource.setLocation(location);
		resource.setDescription(description);
		resource.setHourlyRate(new BigDecimal(hourlyRate));
		resource.setAvailable(true);
		return resource;
	}
}
