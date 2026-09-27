package com.example.booking;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResourceBookingApiApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	private String adminToken;
	private String userToken;

	@BeforeEach
	void authenticate() throws Exception {
		adminToken = login("admin", "Admin@123");
		userToken = login("user", "User@123");
	}

	@Test
	void loginRejectsInvalidCredentials() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userCanReadResourcesButCannotCreateThem() throws Exception {
		mockMvc.perform(get("/api/resources").header("Authorization", bearer(userToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

		mockMvc.perform(post("/api/resources")
						.header("Authorization", bearer(userToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Secret Room","type":"ROOM","hourlyRate":10.00}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanCreateResource() throws Exception {
		mockMvc.perform(post("/api/resources")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Studio B","type":"ROOM","location":"Floor 1","hourlyRate":55.00,"available":true}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Studio B"));
	}

	@Test
	void userCreatesReservationOwnedByJwtSubject() throws Exception {
		long resourceId = firstResourceId();

		mockMvc.perform(post("/api/reservations")
						.header("Authorization", bearer(userToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "resourceId": %s,
								  "startTime": "2026-10-01T10:00:00Z",
								  "endTime": "2026-10-01T12:00:00Z",
								  "notes": "Team sync"
								}
								""".formatted(resourceId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("user"))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.price").exists());
	}

	@Test
	void userCannotSeeAnotherUsersReservations() throws Exception {
		long resourceId = firstResourceId();
		createReservation(adminToken, resourceId, "2026-11-01T10:00:00Z", "2026-11-01T11:00:00Z");

		mockMvc.perform(get("/api/reservations")
						.header("Authorization", bearer(userToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[?(@.username == 'admin')]").isEmpty());
	}

	@Test
	void adminCanFilterPaginateAndSortReservations() throws Exception {
		long resourceId = firstResourceId();
		createReservation(userToken, resourceId, "2026-12-01T09:00:00Z", "2026-12-01T10:00:00Z");
		createReservation(userToken, resourceId, "2026-12-02T09:00:00Z", "2026-12-02T12:00:00Z");

		mockMvc.perform(get("/api/reservations")
						.header("Authorization", bearer(adminToken))
						.param("status", "PENDING")
						.param("minPrice", "1")
						.param("maxPrice", "1000")
						.param("page", "0")
						.param("size", "1")
						.param("sort", "price,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.totalElements").isNumber());
	}

	@Test
	void unauthenticatedRequestsAreRejected() throws Exception {
		mockMvc.perform(get("/api/resources"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userCannotDeleteReservation() throws Exception {
		long resourceId = firstResourceId();
		long reservationId = createReservation(userToken, resourceId, "2026-12-10T09:00:00Z", "2026-12-10T10:00:00Z");

		mockMvc.perform(delete("/api/reservations/" + reservationId)
						.header("Authorization", bearer(userToken)))
				.andExpect(status().isForbidden());
	}

	@Test
	void userCanCancelOwnReservation() throws Exception {
		long resourceId = firstResourceId();
		long reservationId = createReservation(userToken, resourceId, "2026-12-11T09:00:00Z", "2026-12-11T10:00:00Z");

		mockMvc.perform(put("/api/reservations/" + reservationId)
						.header("Authorization", bearer(userToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"CANCELLED"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void validationErrorsReturnBadRequest() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"","password":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details").isArray());
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","password":"%s"}
								""".formatted(username, password)))
				.andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		int start = body.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
		int end = body.indexOf('"', start);
		return body.substring(start, end);
	}

	private long firstResourceId() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/resources").header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		int idIndex = body.indexOf("\"id\":") + 5;
		int comma = body.indexOf(',', idIndex);
		return Long.parseLong(body.substring(idIndex, comma).trim());
	}

	private long createReservation(String token, long resourceId, String start, String end) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/reservations")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resourceId":%s,"startTime":"%s","endTime":"%s"}
								""".formatted(resourceId, start, end)))
				.andExpect(status().isCreated())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		int idIndex = body.indexOf("\"id\":") + 5;
		int comma = body.indexOf(',', idIndex);
		return Long.parseLong(body.substring(idIndex, comma).trim());
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
