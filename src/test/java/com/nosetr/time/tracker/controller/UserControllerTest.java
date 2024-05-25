package com.nosetr.time.tracker.controller;

import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.nosetr.time.tracker.config.SecurityConfig.CustomPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class UserControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	private static final String urlString = "/auth";
	private static String token;

	@Value("${firebase.key}")
	private String firebaseKey;
	@Value("${firebase.userId}")
	private String firebaseUserId;
	@Value("${firebase.email}")
	private String firebaseEmail;
	@Value("${firebase.password}")
	private String firebasePassword;

	@Test
	@Order(1)
	void getUserInfo_withoutAuth_withError() throws Exception {

		webTestClient.get()
				.uri(urlString + "/user")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@Order(2)
	void getUserInfo_withAuth_withSuccess() throws Exception {
		
		String uriString = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword";

		Map<String, Object> userMap = Map.of(
				"email", firebaseEmail, "password", firebasePassword, "returnSecureToken", true
		);

		// Get FIREBASE idToken:
		webTestClient.post()
				.uri(
						new URIBuilder(uriString)
								.addParameter("key", firebaseKey)
								.build()
				)
				.contentType(MediaType.APPLICATION_JSON)
				.body(
						BodyInserters.fromValue(userMap)
				)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody()
				.jsonPath("$.idToken")
				.value(t -> {
					token = (String) t; // Set global token for next tests if we need

					// Make request for test
					webTestClient.get()
							.uri(urlString + "/user")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + t)
							.accept(MediaType.APPLICATION_JSON)
							.exchange()
							.expectStatus()
							.isOk()
							.expectBody(CustomPrincipal.class)
							.consumeWith(response -> {
								CustomPrincipal userDto = response.getResponseBody();

								Assertions.assertNotNull(userDto);

								Assertions.assertEquals(firebaseUserId, userDto.getId());
								Assertions.assertEquals(firebaseEmail, userDto.getName());
							});
				});
	}
}
