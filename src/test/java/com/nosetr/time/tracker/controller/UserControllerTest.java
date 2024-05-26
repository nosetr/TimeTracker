package com.nosetr.time.tracker.controller;

import java.util.Map;
import java.util.Random;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nosetr.time.tracker.config.SecurityConfig.CustomPrincipal;
import com.nosetr.time.tracker.dto.ScoreDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class UserControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ObjectMapper objectMapper;
	
	private static final String urlString = "/auth";
	private static String bearer_token;
	private static String userID;
	private static Integer randomScore;
	private static ScoreDto scoreDto;

	@Value("${firebase.key}")
	private String firebaseKey;
	@Value("${firebase.userId}")
	private String firebaseUserId;
	@Value("${firebase.email}")
	private String firebaseEmail;
	@Value("${firebase.password}")
	private String firebasePassword;
	
	@BeforeEach
	void setUp() {
		Random random = new Random();
    randomScore = random.nextInt(5) + 1; // between 1 and 5
	}

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
					bearer_token = "Bearer " + t; // Set global bearer_token for next tests if we need

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

								userID = userDto.getId();
							});
				});
	}

	@Test
	@Order(3)
	void setScore_withoutAuth_withError() throws Exception {
		
		scoreDto = new ScoreDto();
		scoreDto.setScore(randomScore);
		scoreDto.setVoter(userID);
		scoreDto.setUserId(userID);
		scoreDto.setText("proba");
		
		String valueAsString = objectMapper.writeValueAsString(scoreDto);
		
		webTestClient.post()
				.uri(urlString + "/vote")
				.contentType(MediaType.APPLICATION_JSON)
				.body(
						BodyInserters.fromValue(valueAsString)
				)
				.exchange()
				.expectStatus()
				.isUnauthorized();
	}

	@Test
	@Order(4)
	void setScoreToHimself_withAuth_withError() throws Exception {
		
		String valueAsString = objectMapper.writeValueAsString(scoreDto);
		
		webTestClient.post()
				.uri(urlString + "/vote")
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(
						BodyInserters.fromValue(valueAsString)
				)
				.exchange()
				.expectStatus()
				.isBadRequest()
				.expectBody(String.class)
				.isEqualTo("You can't vote for yourself.");
	}

	@Test
	@Order(4)
	void setScoreToAnotherUser_withAuth_withSuccess() throws Exception {

		scoreDto.setUserId("myTestUserID");
		log.info(scoreDto.toString());
		
		String valueAsString = objectMapper.writeValueAsString(scoreDto);
		
		webTestClient.post()
				.uri(urlString + "/vote")
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(
						BodyInserters.fromValue(valueAsString)
				)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(String.class)
				.isEqualTo("Score successfully set.");
	}
	
	
}
