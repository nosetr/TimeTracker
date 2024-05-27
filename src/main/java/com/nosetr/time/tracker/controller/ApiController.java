package com.nosetr.time.tracker.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuthException;
import com.nosetr.time.tracker.config.SecurityConfig.CustomPrincipal;
import com.nosetr.time.tracker.dto.ScoreDto;
import com.nosetr.time.tracker.dto.TimeRecordDto;
import com.nosetr.time.tracker.dto.TimeRecordResponseDto;
import com.nosetr.time.tracker.dto.UserDto;
import com.nosetr.time.tracker.service.TimeRecordService;
import com.nosetr.time.tracker.service.UserService;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

	private final UserService userService;
	private final TimeRecordService timeRecordService;

	@GetMapping("/user")
	public Mono<CustomPrincipal> getUserInfo(Authentication authentication) {
		CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();

		return Mono.just(customPrincipal);
	}

	@PostMapping("/register")
	public Mono<UserDto> register(@RequestBody @NotNull UserDto userDto) throws FirebaseAuthException {
		// Call registrations service
		return userService.createUser(userDto);
	}

	@PostMapping("/record")
	public Mono<ResponseEntity<String>> saveTimeRecord(
			@RequestBody TimeRecordDto timeRecordDto, Authentication authentication
	) {

		Date today = new Date();
		SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd");
		String formatToday = formattedDate.format(today);

		if (
			timeRecordDto.getDay()
					.compareTo(formatToday) > 0
		) {
			return Mono.just(
					ResponseEntity.badRequest()
							.body("The date can not be after today")
			); // 400 Bad Request
		}

		CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();
		timeRecordDto.setCreator(customPrincipal.getId());

		if (
			timeRecordDto.getUserId() == null || timeRecordDto.getUserId()
					.isEmpty()
		)
			timeRecordDto.setUserId(customPrincipal.getId());

		return timeRecordService.setTimeRecord(timeRecordDto);
	}

	@GetMapping("/record")
	public Flux<TimeRecordResponseDto> getRecordList() throws InterruptedException, ExecutionException {
		return timeRecordService.getAll();
	}

	@PostMapping("/record/{timeRecordId}")
	public Mono<Object> addRating(
			@PathVariable String timeRecordId, @RequestBody ScoreDto scoreDto, Authentication authentication
	) {
		CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();

		return timeRecordService.addRating(
				timeRecordId, scoreDto.toBuilder()
						.voter(customPrincipal.getId())
						.build()
		);
	}

	@GetMapping("/by-week/{year}/{week}")
	public Mono<List<TimeRecordResponseDto>> getTimeRecordsForWeek(@PathVariable int year, @PathVariable int week)
			throws InterruptedException, ExecutionException {
		return timeRecordService.getTimeRecordsForWeek(week, year);
	}
}
