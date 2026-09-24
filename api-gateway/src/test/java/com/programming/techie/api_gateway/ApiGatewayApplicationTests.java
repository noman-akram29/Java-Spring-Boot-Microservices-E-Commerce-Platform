package com.programming.techie.api_gateway;

import com.programming.techie.api_gateway.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"gateway.auth.username=test-gateway",
		"gateway.auth.password=test-password-not-for-prod",
		"gateway.jwt.secret=test-jwt-secret-must-be-at-least-32-chars-long!!",
		"gateway.jwt.expiration-minutes=60"
})
class ApiGatewayApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private JwtService jwtService;

	@Test
	void unauthenticatedRequest_isRejected() {
		webTestClient.get().uri("/api/order")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void healthEndpoint_isPubliclyAccessible_withoutAuth() {
		webTestClient.get().uri("/actuator/health")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	void loginEndpoint_isPubliclyAccessible() {
		webTestClient.post().uri("/auth/login")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.bodyValue("{\"username\":\"test-gateway\",\"password\":\"test-password-not-for-prod\"}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.accessToken").isNotEmpty()
				.jsonPath("$.tokenType").isEqualTo("Bearer");
	}

	@Test
	void authenticatedRequest_withValidJwt_passesAuth() {
		// No backend in this slice test → 5xx is OK; 401 would mean auth failed
		String token = jwtService.createToken("test-gateway");

		webTestClient.get().uri("/api/order")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.exchange()
				.expectStatus().is5xxServerError();
	}

	@Test
	void request_withInvalidJwt_isRejected() {
		webTestClient.get().uri("/api/order")
				.header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.a.valid.jwt")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void login_withWrongPassword_isRejected() {
		webTestClient.post().uri("/auth/login")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.bodyValue("{\"username\":\"test-gateway\",\"password\":\"wrong-password\"}")
				.exchange()
				.expectStatus().isUnauthorized();
	}
}