package com.nosetr.time.tracker.controller;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nosetr.time.tracker.config.SecurityConfig.CustomPrincipal;
import com.nosetr.time.tracker.dto.ScoreDto;
import com.nosetr.time.tracker.dto.TimeRecordDto;
import com.nosetr.time.tracker.dto.TimeRecordResponseDto;
import com.nosetr.time.tracker.dto.UserDto;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class ApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ObjectMapper objectMapper;

	private static final String urlString = "/api";
	private static String bearer_token;
	private static String userID;
	private static ScoreDto scoreDto;
	private static HashSet<String> ownRecordsList = new HashSet<>();
	private static HashSet<String> recordsList = new HashSet<>();

	@Value("${firebase.key}")
	private String firebaseKey;
	@Value("${firebase.userId}")
	private String firebaseUserId;
	@Value("${firebase.email}")
	private String firebaseEmail;
	@Value("${firebase.password}")
	private String firebasePassword;

	/**
	 * Helper to get a random int
	 */
	private static int randomScore() {
		Random random = new Random();
		return random.nextInt(5) + 1; // between 1 and 5
	}

	/**
	 * Helper to create formattedDate
	 */
	private static String createDate(int days) {
		SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, days);
		return formattedDate.format(c.getTime());
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
							});
				});
	}

	@Test
	@Order(3)
	void setTimeRecord_withAuth_withSuccess() throws Exception {

		TimeRecordDto timeRecordDto = new TimeRecordDto()
				.toBuilder()
				.day(createDate(-1)) // yesterday
				.from("09:00")
				.till("17:00")
				.title("study")
				.build();

		String valueAsString = objectMapper.writeValueAsString(timeRecordDto);

		webTestClient.post()
				.uri(urlString + "/record")
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(valueAsString))
				.exchange()
				.expectStatus()
				.isOk();

	}

	@Test
	@Order(4)
	void getListOfTimeRecords_withAuth_withSuccess() throws Exception {
		List<TimeRecordResponseDto> records = webTestClient.get()
				.uri(urlString + "/record")
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBodyList(TimeRecordResponseDto.class)
				.returnResult()
				.getResponseBody();

		Assertions.assertNotNull(records, "The list of scores should not be zero");

		Assertions.assertFalse(records.isEmpty(), "The list of scores should not be empty");

		for (TimeRecordResponseDto record : records) {
			Assertions.assertNotNull(record.getId(), "Each record should have an ID");
			Assertions.assertNotNull(record.getUserId(), "Each record should have a user_id");

			if (firebaseUserId.equals(record.getUserId())) {
				ownRecordsList.add(record.getId());
			} else {
				recordsList.add(record.getId());
			}
		}
	}

	@Test
	@Order(5)
	void setScoreToHimself_withAuth_withError() throws Exception {

		scoreDto = new ScoreDto();
		scoreDto.setScore(randomScore());
		scoreDto.setText("my voting");
		scoreDto.setVoter(firebaseUserId);

		String valueAsString = objectMapper.writeValueAsString(scoreDto);

		for (String timeRecordId : ownRecordsList) {
			webTestClient.post()
					.uri(urlString + "/record/" + timeRecordId)
					.header(HttpHeaders.AUTHORIZATION, bearer_token)
					.contentType(MediaType.APPLICATION_JSON)
					.body(
							BodyInserters.fromValue(valueAsString)
					)
					.exchange()
					.expectStatus()
					.isEqualTo(HttpStatusCode.valueOf(500))
					.expectBody()
					.jsonPath("$.message")
					.isEqualTo("You can't vote for yourself.");
		}
	}

	@Test
	@Order(6)
	void createNewUser_withSuccess() throws Exception {
		UserDto userDto = new UserDto();
		userDto.setEmail((UUID.randomUUID() + "@user.com").substring(24));
		userDto.setPassword(
				UUID.randomUUID()
						.toString()
		);

		String valueAsString = objectMapper.writeValueAsString(userDto);

		webTestClient.post()
				.uri(urlString + "/register")
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(
						BodyInserters.fromValue(valueAsString)
				)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(UserDto.class)
				.consumeWith(response -> {
					UserDto newUser = response.getResponseBody();

					Assertions.assertNotNull(newUser);

					Assertions.assertEquals(newUser.getEmail(), userDto.getEmail());
					Assertions.assertTrue(newUser.isEmailVerified());

					userID = newUser.getId();
				});
	}

	@Test
	@Order(7)
	void setTimeRecordToNewUser_withAuth_withError() throws Exception {

		TimeRecordDto timeRecordDto = new TimeRecordDto()
				.toBuilder()
				.day(createDate(1)) // tomorrow
				.from("08:00")
				.till("13:00")
				.title("traineeship")
				.build();

		String valueAsString = objectMapper.writeValueAsString(timeRecordDto);

		webTestClient.post()
				.uri(urlString + "/record")
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(valueAsString))
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.BAD_REQUEST)
				.expectBody(String.class)
				.isEqualTo("The date can not be after today");
	}

	@Test
	@Order(8)
	void setTimeRecordToNewUser_withAuth_withSuccess() throws Exception {

		TimeRecordDto timeRecordDto = new TimeRecordDto()
				.toBuilder()
				.userId(userID)
				.day(createDate(0)) // today
				.from("08:00")
				.till("13:00")
				.title("traineeship")
				.build();

		String valueAsString = objectMapper.writeValueAsString(timeRecordDto);

		webTestClient.post()
				.uri(urlString + "/record")
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(valueAsString))
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(String.class)
				.isEqualTo("Time record successfully set.");
	}

	@Test
	@Order(9)
	void getSecondListOfTimeRecords_withAuth_withSuccess() throws Exception {
		List<TimeRecordResponseDto> records = webTestClient.get()
				.uri(urlString + "/record")
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBodyList(TimeRecordResponseDto.class)
				.returnResult()
				.getResponseBody();

		Assertions.assertNotNull(records, "The list of records should not be zero");

		Assertions.assertFalse(records.isEmpty(), "The list of records should not be empty");

		recordsList = new HashSet<>();
		for (TimeRecordResponseDto record : records) {
			Assertions.assertNotNull(record.getId(), "Each record should have an ID");
			Assertions.assertNotNull(record.getUserId(), "Each record should have a user_id");

			if (firebaseUserId.equals(record.getUserId())) {
				ownRecordsList.add(record.getId());
			} else {
				if (record.getRatings() == null)
					recordsList.add(record.getId());
			}
		}
	}

	@Test
	@Order(10)
	void setScoreToAnotherUser_withAuth_withSuccess() throws Exception {

		scoreDto = new ScoreDto();
		scoreDto.setScore(randomScore());
		scoreDto.setText("my next voting");

		String valueAsString = objectMapper.writeValueAsString(scoreDto);

		for (String timeRecordId : recordsList) {
			webTestClient.post()
					.uri(urlString + "/record/" + timeRecordId)
					.header(HttpHeaders.AUTHORIZATION, bearer_token)
					.contentType(MediaType.APPLICATION_JSON)
					.body(
							BodyInserters.fromValue(valueAsString)
					)
					.exchange()
					.expectStatus()
					.isOk();
		}
	}

	@Test
	@Order(11)
	void getRecordsOfTheWeek_withAuth_withSuccess() throws Exception {
		LocalDate now = LocalDate.now();
		int year = now.getYear();
		int week = now.get(
				WeekFields.of(Locale.getDefault())
						.weekOfYear()
		);

		List<TimeRecordResponseDto> records = webTestClient.get()
				.uri(String.format("%s/by-week/%d/%d", urlString, year, week))
				.header(HttpHeaders.AUTHORIZATION, bearer_token)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBodyList(TimeRecordResponseDto.class)
				.returnResult()
				.getResponseBody();

		Assertions.assertNotNull(records, "The list of records should not be zero");

		Assertions.assertFalse(records.isEmpty(), "The list of records should not be empty");

		for (TimeRecordResponseDto record : records) {
			Assertions.assertNotNull(record.getId(), "Each record should have an ID");
			Assertions.assertNotNull(record.getUserId(), "Each record should have a user_id");

			if (firebaseUserId.equals(record.getUserId())) {
				ownRecordsList.add(record.getId());
			} else {
				recordsList.add(record.getId());
			}
		}
	}

}
