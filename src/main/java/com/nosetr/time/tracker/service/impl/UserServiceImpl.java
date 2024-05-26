package com.nosetr.time.tracker.service.impl;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.cloud.FirestoreClient;
import com.nosetr.time.tracker.dto.ScoreDto;
import com.nosetr.time.tracker.dto.UserDto;
import com.nosetr.time.tracker.service.UserService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final FirebaseAuth firebaseAuth;

	@Override
	public Mono<UserRecord> createUser(UserDto userDto) throws FirebaseAuthException {

		CreateRequest request = new CreateRequest();
		request.setEmail(userDto.getEmail());
		request.setPassword(userDto.getPassword());
		request.setEmailVerified(Boolean.TRUE);

		return Mono.just(firebaseAuth.createUser(request))
				.onErrorResume(e -> Mono.error(e));
	}

	@Override
	public Mono<Date> setScore(ScoreDto scoreDto) throws InterruptedException, ExecutionException {
		Firestore firestore = FirestoreClient.getFirestore();

		DocumentReference reference = firestore.collection("user_scores")
				.document();
		scoreDto.setId(reference.getId());
		ApiFuture<WriteResult> future = reference.set(scoreDto);

		return Mono.just(
				future.get()
						.getUpdateTime()
						.toDate()
		);
	}

}
