package com.nosetr.time.tracker.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.nosetr.time.tracker.service.FirebaseAuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter class to check incoming requests.
 */
@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

	@Autowired
	private FirebaseAuthService firebaseAuthService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = request.getHeader("Authorization");
		if (token != null && token.startsWith("Bearer ")) {
			token = token.substring(7);
			try {
				FirebaseToken decodedToken = firebaseAuthService.verifyToken(token);
				// Put user information in the security context
				request.setAttribute("firebaseToken", decodedToken);
			} catch (FirebaseAuthException e) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		}
		filterChain.doFilter(request, response);
	}
}
