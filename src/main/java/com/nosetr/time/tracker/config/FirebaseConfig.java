package com.nosetr.time.tracker.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
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

	@Value("${firebase.key}")
	private String firebaseKey;
	
	@Bean
	public FirebaseApp initializeFirebase() throws IOException {
		FileInputStream serviceAccount = new FileInputStream("src/main/resources/firebase-service-account.json");

		FirebaseOptions options = FirebaseOptions.builder()
				.setCredentials(GoogleCredentials.fromStream(serviceAccount))
				.build();

		return (FirebaseApp.getApps()
				.isEmpty()) ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
	}
}
