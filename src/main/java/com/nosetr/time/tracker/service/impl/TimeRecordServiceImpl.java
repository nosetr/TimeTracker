package com.nosetr.time.tracker.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.nosetr.time.tracker.dto.ScoreDto;
import com.nosetr.time.tracker.dto.TimeRecordDto;
import com.nosetr.time.tracker.dto.TimeRecordResponseDto;
import com.nosetr.time.tracker.service.TimeRecordService;
import com.nosetr.time.tracker.util.ApiFutureUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TimeRecordServiceImpl implements TimeRecordService {

	private static final Firestore db = FirestoreClient.getFirestore();

	@Override
	public Mono<ResponseEntity<String>> setTimeRecord(TimeRecordDto timeRecordDto) {

		return Mono.fromCallable(() -> {
			db.collection("time_records")
					.add(timeRecordDto);
			return "Time record successfully set.";
		})
				.map(message -> ResponseEntity.ok(message)) // 200 OK
				.onErrorResume(e -> {
					return Mono.just(
							ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
									.body("Failed to set time record: " + e.getMessage())
					); // 500 Internal Server Error
				});
	}

	@Override
	public Mono<Object> addRating(String timeRecordId, ScoreDto scoreDto) {
		DocumentReference reference = db.collection("time_records")
				.document(timeRecordId);

		return Mono.fromFuture(ApiFutureUtil.toCompletableFuture(reference.get()))
				.flatMap(documentSnapshot -> {
					if (!documentSnapshot.exists()) { return Mono.error(new RuntimeException("TimeRecord not found.")); }

					TimeRecordDto timeRecordDto = documentSnapshot.toObject(TimeRecordDto.class);

					if (
						scoreDto.getVoter()
								.equals(timeRecordDto.getUserId())
					) {
						return Mono.error(
								new RuntimeException("You can't vote for yourself.")
						);
					}

					List<ScoreDto> ratings = (timeRecordDto.getRatings() == null)
							? new ArrayList<>()
							: timeRecordDto.getRatings();

					if (!ratings.isEmpty()) {
						boolean userAlreadyVoted = ratings.stream()
								.anyMatch(
										r -> r.getVoter()
												.equals(scoreDto.getVoter())
								);

						if (userAlreadyVoted) { return Mono.error(new RuntimeException("User has already voted.")); }
					}

					ratings.add(scoreDto);
					timeRecordDto.setRatings(ratings);
					
					int size = ratings.size();
					int score = 0;
					for (ScoreDto rating : ratings) {
						score += rating.getScore();
					}
					
					timeRecordDto.setAverageRating(score / size);

					return Mono.fromFuture(ApiFutureUtil.toCompletableFuture(reference.set(timeRecordDto)))
							.thenReturn("Rating added successfully");
				});
	}

	@Override
	public Flux<TimeRecordResponseDto> getAll() throws InterruptedException, ExecutionException {

		ApiFuture<QuerySnapshot> apiFuture = db.collection("time_records")
				.get();
		List<QueryDocumentSnapshot> documents = apiFuture.get()
				.getDocuments();

		return Flux.fromStream(
				documents.stream()
						.map(doc -> {
							TimeRecordResponseDto dto = doc.toObject(TimeRecordResponseDto.class);
							dto.setId(doc.getId());
							return dto;
						})
		);

	}

	@Override
	public Mono<List<TimeRecordResponseDto>> getTimeRecordsForWeek(int weekOfYear, int year) throws InterruptedException, ExecutionException {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		return getAll()
				.filter(record -> {
					LocalDate date = LocalDate.parse(record.getDay(), formatter);
					int recordWeek = date.get(
							WeekFields.of(Locale.getDefault())
									.weekOfYear()
					);
					int recordYear = date.getYear();
					return recordWeek == weekOfYear && recordYear == year;
				})
				.sort((r1, r2) -> {
					LocalDate date1 = LocalDate.parse(r1.getDay(), formatter);
					LocalDate date2 = LocalDate.parse(r2.getDay(), formatter);
					return date1.compareTo(date2);
				})
				.collectList();
	}

}
