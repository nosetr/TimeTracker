package com.nosetr.time.tracker.service;

import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

/**
 * Service to authenticate users and verify tokens.
 */
@Service
public class FirebaseAuthService {

	public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
		return FirebaseAuth.getInstance()
				.verifyIdToken(idToken);
	}
}
