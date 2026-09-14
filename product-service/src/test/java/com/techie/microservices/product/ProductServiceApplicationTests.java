package com.techie.microservices.product;

import com.techie.microservices.product.external.client.InventoryClient;
import com.techie.microservices.product.external.dto.InventoryRequest;
import com.techie.microservices.product.external.dto.InventoryResponse;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests {

	@LocalServerPort
	private Integer port;

	@MockitoBean
	private InventoryClient inventoryClient;

	@BeforeEach
	void setUp() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;

		// Product create calls inventory; mock a successful upsert
		when(inventoryClient.upsertInventory(any(InventoryRequest.class)))
				.thenAnswer(invocation -> {
					InventoryRequest req = invocation.getArgument(0);
					return ResponseEntity.ok(new InventoryResponse(1L, req.skuCode(), req.quantity()));
				});
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

		// Controller is mapped at "/" (gateway strips /api/product)
		RestAssured.given()
				.contentType("application/json")
				.body(requestBody)
				.when().post("/")
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
				.when().post("/")
				.then().statusCode(400);
	}
}