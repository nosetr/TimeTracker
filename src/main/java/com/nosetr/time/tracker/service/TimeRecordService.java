package com.nosetr.time.tracker.service;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.http.ResponseEntity;

import com.nosetr.time.tracker.dto.ScoreDto;
import com.nosetr.time.tracker.dto.TimeRecordDto;
import com.nosetr.time.tracker.dto.TimeRecordResponseDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TimeRecordService {

	Mono<ResponseEntity<String>> setTimeRecord(TimeRecordDto timeRecordDto);

	Mono<Object> addRating(String timeRecordId, ScoreDto scoreDto);

	Flux<TimeRecordResponseDto> getAll() throws InterruptedException, ExecutionException;

	Mono<List<TimeRecordResponseDto>> getTimeRecordsForWeek(int weekOfYear, int year)
			throws InterruptedException, ExecutionException;

}
