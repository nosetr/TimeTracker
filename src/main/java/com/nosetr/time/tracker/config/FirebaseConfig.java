package com.nosetr.time.tracker.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Configuration class to initialize the Firebase Admin SDK.
 */
@Configuration
public class FirebaseConfig {

	@SuppressWarnings("deprecation")
	@Bean
	public FirebaseApp initializeFirebaseApp() throws IOException {
		FileInputStream serviceAccount = new FileInputStream("src/main/resources/firebase-serviceAccountKey.json");

		FirebaseOptions options = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(serviceAccount))
				.build();

		return FirebaseApp.initializeApp(options);
	}
}
