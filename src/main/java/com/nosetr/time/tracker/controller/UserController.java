package com.nosetr.time.tracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuthException;
import com.nosetr.time.tracker.config.SecurityConfig.CustomPrincipal;
import com.nosetr.time.tracker.dto.ScoreDto;
import com.nosetr.time.tracker.dto.UserDto;
import com.nosetr.time.tracker.service.UserService;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

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

	@PostMapping("/vote")
	public Mono<ResponseEntity<String>> setScore(@RequestBody @NotNull ScoreDto scoreDto, Authentication authentication) {
		CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();

    if (scoreDto.getUserId().equals(customPrincipal.getId())) {
        return Mono.just(ResponseEntity.badRequest().body("You can't vote for yourself.")); // 400 Bad Request
    }

    return Mono.fromCallable(() -> {
        userService.setScore(
                scoreDto.toBuilder()
                        .voter(customPrincipal.getId())
                        .build()
        ).toFuture().get(); // converts Mono to Future and calls get to get Date
        return "Score successfully set.";
    })
    .map(message -> ResponseEntity.ok(message)) // 200 OK
    .onErrorResume(e -> {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to set score: " + e.getMessage())); // 500 Internal Server Error
    });
	}
}
