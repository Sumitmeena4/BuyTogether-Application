package com.hackathon.gemma_app.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.hackathon.gemma_app.exception.AiServiceException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class GemmaServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsStructuredExtractionRequestToConfiguredGemmaModel() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        doReturn(200).when(httpResponse).statusCode();
        doReturn("""
                {"candidates":[{"content":{"parts":[{"text":"{\\"requests\\":[]}"}]}}]}
                """).when(httpResponse).body();
        doReturn(httpResponse).when(httpClient).send(
                any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        GemmaService service = new GemmaService(httpClient, objectMapper,
                new GemmaResponseParser(objectMapper), "server-secret", "gemma-4-test");

        assertEquals(List.of(), service.analyze("Rahul: I need some pens."));

        var captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = captor.getValue();
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemma-4-test:generateContent",
                sent.uri().toString());
        assertEquals("server-secret", sent.headers().firstValue("x-goog-api-key").orElseThrow());
        assertTrue(sent.uri().getQuery() == null);
        assertTrue(sent.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));

        JsonNode capturedBody = bodyAsJson(sent);
        JsonNode generationConfig = capturedBody.path("generationConfig");
        assertEquals("application/json", generationConfig.path("responseMimeType").asText());
        assertEquals("ARRAY", generationConfig.path("responseSchema")
                .path("properties").path("requests").path("type").asText());
        assertEquals("INTEGER", generationConfig.path("responseSchema")
                .path("properties").path("requests").path("items")
                .path("properties").path("quantity").path("type").asText());
        assertTrue(generationConfig.path("responseSchema")
                .path("properties").path("requests").path("items")
                .path("properties").path("quantity").path("nullable").asBoolean());
    }

    @Test
    void refusesToCallGeminiWhenNoServerApiKeyIsConfigured() {
        HttpClient httpClient = mock(HttpClient.class);
        GemmaService service = new GemmaService(httpClient, objectMapper,
                new GemmaResponseParser(objectMapper), "", "gemma-4-test");

        assertThrows(AiServiceException.class, () -> service.analyze("some chat"));
        verifyNoInteractions(httpClient);
    }

    private JsonNode bodyAsJson(HttpRequest request) {
        var publisher = request.bodyPublisher().orElseThrow();
        var captured = new java.util.concurrent.atomic.AtomicReference<String>();
        publisher.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
            private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();

            @Override
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(java.nio.ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }

            @Override
            public void onError(Throwable throwable) {
                throw new AssertionError(throwable);
            }

            @Override
            public void onComplete() {
                captured.set(bytes.toString(java.nio.charset.StandardCharsets.UTF_8));
            }
        });
        return objectMapper.readTree(captured.get());
    }
}
