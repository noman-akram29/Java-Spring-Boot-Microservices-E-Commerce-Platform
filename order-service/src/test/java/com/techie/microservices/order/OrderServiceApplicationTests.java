package com.techie.microservices.order;

import com.techie.microservices.order.stubs.InventoryClientStub;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "inventory.service.url=http://localhost:8089"
})
@EnableWireMock({
        @ConfigureWireMock(port = 8089)
})
@Testcontainers
class OrderServiceApplicationTests {

    @Container
    static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                kafkaContainer::getBootstrapServers);
    }

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
                .post("/")
                .then()
                .log().ifValidationFails()
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
                .when()
                .post("/")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .extract()
                .path("orderNumber");

        RestAssured.given()
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body(submitOrderJson)
                .when()
                .post("/")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("orderNumber", Matchers.equalTo(firstOrderNumber))
                .body("message", Matchers.containsString("replay"));
    }

    @Test
    void concurrentRequestsWithSameIdempotencyKey_mustOnlyDecrementInventoryOnce()
            throws InterruptedException {

        InventoryClientStub.stubDecreaseInventory("race_sku", 1);

        String idempotencyKey = UUID.randomUUID().toString();

        String submitOrderJson = """
                {
                    "skuCode": "race_sku",
                    "price": 20,
                    "quantity": 1,
                    "userDetails": {
                        "email": "test@example.com",
                        "firstName": "Test",
                        "lastName": "User"
                    }
                }
                """;

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(2);

        Runnable placeOrder = () -> {
            try {
                startLine.await();

                RestAssured.given()
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType("application/json")
                        .body(submitOrderJson)
                        .when()
                        .post("/");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLine.countDown();
            }
        };

        try {
            pool.submit(placeOrder);
            pool.submit(placeOrder);

            startLine.countDown();

            boolean completed = finishLine.await(10, TimeUnit.SECONDS);

            if (!completed) {
                throw new AssertionError(
                        "Concurrent order requests did not finish within 10 seconds");
            }
        } finally {
            pool.shutdownNow();
        }

        WireMock.verify(
                1,
                WireMock.postRequestedFor(WireMock.urlEqualTo("/decrease"))
                        .withRequestBody(
                                WireMock.matchingJsonPath(
                                        "$.skuCode",
                                        WireMock.equalTo("race_sku"))));
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
                .when()
                .post("/")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("status", Matchers.equalTo("FAILED"))
                .body("orderNumber", Matchers.nullValue());
    }
}
