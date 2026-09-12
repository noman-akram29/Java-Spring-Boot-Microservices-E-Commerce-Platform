package com.techie.microservices.order.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class InventoryClientStub {

    public static void stubDecreaseInventory(String skuCode, Integer quantity) {
        stubFor(post(urlEqualTo("/decrease"))
                .withRequestBody(matchingJsonPath("$.skuCode", equalTo(skuCode)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":1,"skuCode":"%s","quantity":%d}
                                """.formatted(skuCode, quantity))));
    }

    public static void stubInventoryFailure(String skuCode) {
        stubFor(post(urlEqualTo("/decrease"))
                .withRequestBody(matchingJsonPath("$.skuCode", equalTo(skuCode)))
                .willReturn(aResponse().withStatus(400)));
    }
}