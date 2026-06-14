package it.aruba.qaa.healing.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.model.HealingRequest;
import it.aruba.qaa.healing.model.HealingResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class ArubaHealerClient implements HealerClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final java.time.Duration timeout;

    public ArubaHealerClient(ArubaHealingConfig config) {
        this(
                HttpClient.newBuilder().connectTimeout(config.timeout()).build(),
                new ObjectMapper(),
                config.locatorEndpoint(),
                config.timeout()
        );
    }

    ArubaHealerClient(HttpClient httpClient, ObjectMapper objectMapper, URI endpoint, java.time.Duration timeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.timeout = timeout;
    }

    @Override
    public HealingResponse heal(HealingRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return HealingResponse.empty();
            }
            return objectMapper.readValue(response.body(), HealingResponse.class);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return HealingResponse.empty();
        }
    }
}
