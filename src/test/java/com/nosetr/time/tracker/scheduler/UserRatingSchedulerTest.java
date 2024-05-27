package com.nosetr.time.tracker.scheduler;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SpringBootTest
class UserRatingSchedulerTest {

	@SpyBean
	private UserRatingScheduler userRatingScheduler;

	@Test
	public void reportCurrentTime() {
		Awaitility.await()
				.atMost(Durations.TEN_SECONDS)
				.untilAsserted(() -> {
					verify(userRatingScheduler, atLeast(2)).updateTopUsers();
				});
	}

}
