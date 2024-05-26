package com.nosetr.time.tracker.service;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.nosetr.time.tracker.dto.ScoreDto;
import com.nosetr.time.tracker.dto.UserDto;

import reactor.core.publisher.Mono;

public interface UserService {
	
	Mono<UserRecord> createUser(UserDto userEntity) throws FirebaseAuthException;

	Mono<Date> setScore(ScoreDto scoreDto) throws InterruptedException, ExecutionException;

}
