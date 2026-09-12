package com.techie.microservices.product;

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests {

	@LocalServerPort
	private Integer port;

	@BeforeEach
	void setUp() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
	}

	@Test
	void shouldCreateProduct() {
		String sku = "iphone_" + System.currentTimeMillis();

		String requestBody = """
				{
				    "skuCode": "%s",
				    "name": "iPhone 15",
				    "description": "iPhone 15 is a smartphone from Apple.",
				    "price": 1000,
				    "quantity": 10
				}
				""".formatted(sku);

		RestAssured.given()
				.contentType("application/json")
				.body(requestBody)
				.when().post("/api/product")
				.then().statusCode(201)
				.body("id", Matchers.notNullValue())
				.body("name", Matchers.equalTo("iPhone 15"))
				.body("quantity", Matchers.equalTo(10));
	}

	@Test
	void shouldRejectInvalidProduct_missingRequiredFields() {
		String invalidBody = """
				{
				    "skuCode": "",
				    "price": -5
				}
				""";

		RestAssured.given()
				.contentType("application/json")
				.body(invalidBody)
				.when().post("/api/product")
				.then().statusCode(400);
	}
}