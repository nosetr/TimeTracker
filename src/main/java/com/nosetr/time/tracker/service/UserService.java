package com.nosetr.time.tracker.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.nosetr.time.tracker.dto.UserDto;

import reactor.core.publisher.Mono;

public interface UserService {
	
	Mono<UserDto> createUser(UserDto userDto) throws FirebaseAuthException;

}
