package com.engine.activity;

import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class HttpActivityHandler implements ActivityHandler{
    private final RestClient restClient;

    public HttpActivityHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public String getActivityName() {
        return "HTTP_CALL"; // The key stored in the registry
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> inputPayload) throws Exception {
        String url = (String) inputPayload.get("url");
        String method = (String) inputPayload.getOrDefault("method", "POST");
        Object body = inputPayload.get("body");

        return restClient.method(HttpMethod.valueOf(method.toUpperCase()))
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body != null ? body : Map.of())
                .retrieve()
                .body(Map.class); // Captures response body directly as Map output
    }
    
}
