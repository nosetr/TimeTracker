package com.nosetr.time.tracker.service.impl;

import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.nosetr.time.tracker.dto.UserDto;
import com.nosetr.time.tracker.service.UserService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final FirebaseAuth firebaseAuth;

	@Override
	public Mono<UserDto> createUser(UserDto userDto) throws FirebaseAuthException {

		CreateRequest request = new CreateRequest();
		request.setEmail(userDto.getEmail());
		request.setPassword(userDto.getPassword());
		request.setEmailVerified(Boolean.TRUE);

		return Mono.fromCallable(() -> firebaseAuth.createUser(request))
				.map(
						userRecord -> new UserDto().toBuilder()
								.id(userRecord.getUid())
								.email(userRecord.getEmail())
								.emailVerified(userRecord.isEmailVerified())
								.build()
				)
				.onErrorMap(FirebaseAuthException.class, e -> {
					return new RuntimeException("Failed to create user", e);
				});
	}

	/////////////////////////////////////////////////////

//	@Override
//	public Mono<Date> setScore(ScoreDto scoreDto) throws InterruptedException, ExecutionException {
//		Firestore firestore = FirestoreClient.getFirestore();
//
//		DocumentReference reference = firestore.collection("user_scores")
//				.document();
//		scoreDto.setId(reference.getId());
//		ApiFuture<WriteResult> future = reference.set(scoreDto);
//
//		return Mono.just(
//				future.get()
//						.getUpdateTime()
//						.toDate()
//		);
//	}
//
//	@Override
//	public Mono<ApiFuture<QuerySnapshot>> getScoreByKey(String key, String value) {
//
//		Firestore firestore = FirestoreClient.getFirestore();
//		//		ApiFuture<QuerySnapshot> apiFuture = firestore.collection("user_scores")
//		//				.whereEqualTo(key, value)
//		//				.limit(1)
//		//				.get();
//
//		return Mono.fromCallable(
//				() -> firestore.collection("user_scores")
//						.whereEqualTo(key, value)
//						.limit(1)
//						.get()
//		);
//	}
}
