package com.techie.microservices.order;

import com.techie.microservices.order.stubs.InventoryClientStub;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;

import java.util.UUID;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class OrderServiceApplicationTests {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void shouldSubmitOrderSuccessfully() {
        InventoryClientStub.stubDecreaseInventory("iphone_15", 1);

        String submitOrderJson = """
                {
                    "skuCode": "iphone_15",
                    "price": 1000,
                    "quantity": 1,
                    "userDetails": {
                        "email": "test@example.com",
                        "firstName": "Test",
                        "lastName": "User"
                    }
                }
                """;

        RestAssured.given()
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .statusCode(201)
                .body("status", Matchers.equalTo("SUCCESS"))
                .body("orderNumber", Matchers.notNullValue());
    }

    @Test
    void shouldReplayIdempotently_whenSameKeySentTwice() {
        InventoryClientStub.stubDecreaseInventory("charger_x1", 2);
        String idempotencyKey = UUID.randomUUID().toString();

        String submitOrderJson = """
                {
                    "skuCode": "charger_x1",
                    "price": 50,
                    "quantity": 2,
                    "userDetails": {
                        "email": "test@example.com",
                        "firstName": "Test",
                        "lastName": "User"
                    }
                }
                """;

        String firstOrderNumber = RestAssured.given()
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body(submitOrderJson)
                .when().post("/api/order")
                .then().statusCode(201)
                .extract().path("orderNumber");

        // second call with the SAME key must return the SAME order, not create a new
        // one
        RestAssured.given()
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body(submitOrderJson)
                .when().post("/api/order")
                .then().statusCode(201)
                .body("orderNumber", Matchers.equalTo(firstOrderNumber))
                .body("message", Matchers.containsString("replay"));
    }

    @Test
    void shouldFailGracefully_whenInventoryServiceReturnsError() {
        InventoryClientStub.stubInventoryFailure("out_of_stock_sku");

        String submitOrderJson = """
                {
                    "skuCode": "out_of_stock_sku",
                    "price": 10,
                    "quantity": 5,
                    "userDetails": {
                        "email": "test@example.com",
                        "firstName": "Test",
                        "lastName": "User"
                    }
                }
                """;

        RestAssured.given()
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .body(submitOrderJson)
                .when().post("/api/order")
                .then().statusCode(201) // controller always returns 201; failure is signalled in the body
                .body("status", Matchers.equalTo("FAILED"))
                .body("orderNumber", Matchers.nullValue());
    }
}