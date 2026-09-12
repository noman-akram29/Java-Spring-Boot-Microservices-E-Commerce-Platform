package com.techie.microservices.inventory;

import com.techie.microservices.inventory.dto.InventoryRequest;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InventoryServiceApplicationTests {

	@LocalServerPort
	private Integer port;

	@BeforeEach
	void setUp() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
	}

	@Test
	void shouldCreateThenDecreaseInventoryEndToEnd() {
		String sku = "sku-" + System.currentTimeMillis();

		RestAssured.given()
				.contentType("application/json")
				.body(new InventoryRequest(sku, 10))
				.when().post("/")
				.then().statusCode(200)
				.body("quantity", Matchers.equalTo(10));

		RestAssured.given()
				.contentType("application/json")
				.body(new InventoryRequest(sku, 4))
				.when().post("/decrease")
				.then().statusCode(200)
				.body("quantity", Matchers.equalTo(6));

		RestAssured.given()
				.when().get("/" + sku)
				.then().statusCode(200)
				.body("quantity", Matchers.equalTo(6));
	}

	@Test
	void shouldReturn400WhenDecreasingBelowAvailableStock() {
		String sku = "sku-low-" + System.currentTimeMillis();

		RestAssured.given()
				.contentType("application/json")
				.body(new InventoryRequest(sku, 2))
				.when().post("/")
				.then().statusCode(200);

		RestAssured.given()
				.contentType("application/json")
				.body(new InventoryRequest(sku, 5))
				.when().post("/decrease")
				.then().statusCode(400);
	}
}