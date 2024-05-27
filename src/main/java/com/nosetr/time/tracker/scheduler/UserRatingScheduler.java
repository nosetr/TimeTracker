package com.nosetr.time.tracker.scheduler;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import com.nosetr.time.tracker.dto.TimeRecordResponseDto;
import com.nosetr.time.tracker.util.ApiFutureUtil;

import reactor.core.publisher.Mono;

@Service
public class UserRatingScheduler {

	private static final Firestore db = FirestoreClient.getFirestore();

	@Scheduled(cron = "0 0 1 * * ?") // Each day at 01:00 Hour
	public void updateTopUsers() {
		deleteOldTopUsers()
				.then(getTop10Users())
				.flatMap(this::saveTop10Users)
				.subscribe(
						success -> System.out.println("Top 10 users updated successfully!"), error -> System.err
								.println("Error updating top 10 users: " + error.getMessage())
				);
	}

	private Mono<List<TimeRecordResponseDto>> getTop10Users() {
		ApiFuture<QuerySnapshot> querySnapshotFuture = db.collection("time_records")
				.orderBy("averageRating", Query.Direction.DESCENDING)
				.limit(10)
				.get();

		return Mono.fromFuture(ApiFutureUtil.toCompletableFuture(querySnapshotFuture))
				.flatMap(querySnapshot -> {
					List<TimeRecordResponseDto> topUsers = querySnapshot.getDocuments()
							.stream()
							.map(doc -> {
								TimeRecordResponseDto dto = doc.toObject(TimeRecordResponseDto.class);
								dto.setId(doc.getId());
								return dto;
							})
							.collect(Collectors.toList());
					return Mono.just(topUsers);
				});
	}

	private Mono<WriteResult> saveTop10Users(List<TimeRecordResponseDto> topUsers) {
		DocumentReference documentReference = db.collection("top_users")
				.document("latest");
		ApiFuture<WriteResult> writeResultFuture = documentReference.set(topUsers);
		return Mono.fromFuture(ApiFutureUtil.toCompletableFuture(writeResultFuture));
	}

	private Mono<WriteResult> deleteOldTopUsers() {
		DocumentReference documentReference = db.collection("top_users")
				.document("latest");
		ApiFuture<WriteResult> deleteResultFuture = documentReference.delete();
		return Mono.fromFuture(ApiFutureUtil.toCompletableFuture(deleteResultFuture));
	}
}
