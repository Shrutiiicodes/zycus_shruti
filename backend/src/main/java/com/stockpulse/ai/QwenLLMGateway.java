package com.stockpulse.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat completions call. If your Qwen endpoint's auth header,
 * path, or response shape differs, this is the only method that needs to change.
 */
@Component("qwen")
public class QwenLLMGateway implements LLMGateway {

    @Value("${llm.api-key:}")
    private String apiKey;
    @Value("${llm.model}")
    private String model;
    @Value("${llm.base-url}")
    private String baseUrl;

    private final RestClient http = RestClient.builder()
            .requestFactory(clientRequestFactory())
            .build();

    @Override
    @SuppressWarnings("unchecked")
    public String call(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3
        );

        Map<String, Object> response = http.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private org.springframework.http.client.ClientHttpRequestFactory clientRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());
        return factory;
    }
}