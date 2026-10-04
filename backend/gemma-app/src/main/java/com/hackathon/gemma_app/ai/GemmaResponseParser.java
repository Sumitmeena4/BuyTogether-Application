package com.hackathon.gemma_app.ai;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.hackathon.gemma_app.dto.GemmaRequestDto;
import com.hackathon.gemma_app.dto.GemmaResponseDto;
import com.hackathon.gemma_app.exception.InvalidAiResponseException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GemmaResponseParser {
    private final ObjectMapper objectMapper;

    public GemmaResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<GemmaRequestDto> parse(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            throw new InvalidAiResponseException("Gemma returned an empty response.");
        }
        String json = responseText.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int firstBrace = json.indexOf('{');
        int lastBrace = json.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace >= firstBrace) {
            json = json.substring(firstBrace, lastBrace + 1);
        }
        try {
            GemmaResponseDto response = objectMapper.readValue(json, GemmaResponseDto.class);
            if (response.requests() == null) {
                throw new InvalidAiResponseException("Gemma response is missing the requests array.");
            }
            return response.requests();
        } catch (JacksonException exception) {
            throw new InvalidAiResponseException("Gemma returned invalid JSON.", exception);
        }
    }
}
