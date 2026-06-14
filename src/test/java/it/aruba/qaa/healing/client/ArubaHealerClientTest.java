package it.aruba.qaa.healing.client;

import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.config.HealingMode;
import it.aruba.qaa.healing.model.HealingRequest;
import it.aruba.qaa.healing.model.HealingResponse;
import it.aruba.qaa.healing.model.LocatorPayload;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArubaHealerClientTest {

    @Test
    void postsLocatorRequestAndReadsCandidates() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "status": "candidate_found",
                              "candidates": [
                                {
                                  "strategy": "cssSelector",
                                  "value": "button[data-testid='confirm']",
                                  "confidence": 0.96,
                                  "reason": "stable test id"
                                }
                              ]
                            }
                            """));
            server.start();

            ArubaHealerClient client = new ArubaHealerClient(config(server.url("/").uri()));
            HealingResponse response = client.heal(request());

            assertEquals("candidate_found", response.status());
            assertEquals(1, response.candidates().size());
            assertEquals("button[data-testid='confirm']", response.candidates().get(0).value());
            assertEquals("/heal/locator", server.takeRequest().getPath());
        }
    }

    @Test
    void nonSuccessfulResponsesReturnEmptyResult() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500));
            server.start();

            ArubaHealerClient client = new ArubaHealerClient(config(server.url("/").uri()));
            HealingResponse response = client.heal(request());

            assertEquals("empty", response.status());
            assertEquals(0, response.candidates().size());
        }
    }

    private static ArubaHealingConfig config(URI serverUri) {
        return new ArubaHealingConfig(
                HealingMode.ASSISTED_HEAL,
                serverUri,
                0.90,
                Set.of("cssselector", "xpath"),
                Path.of("target/test-healing-report"),
                true,
                Duration.ofMillis(500)
        );
    }

    private static HealingRequest request() {
        return new HealingRequest(
                "CheckoutTest.shouldConfirm",
                "CheckoutPage",
                "https://app.test/checkout",
                new LocatorPayload("cssSelector", "#confirm"),
                "<html></html>",
                null,
                "NoSuchElementException"
        );
    }
}
