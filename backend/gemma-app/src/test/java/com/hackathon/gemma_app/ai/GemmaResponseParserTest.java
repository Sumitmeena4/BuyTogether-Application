package com.hackathon.gemma_app.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hackathon.gemma_app.exception.InvalidAiResponseException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GemmaResponseParserTest {
    private final GemmaResponseParser parser = new GemmaResponseParser(new ObjectMapper());

    @Test
    void parsesGemmaJsonAndNullQuantity() {
        var requests = parser.parse("""
                ```json
                {"requests":[{"person":"Rahul","originalText":"I need some pens.",
                "item":"pens","normalizedItem":"Pen","quantity":null,"unit":"pieces",
                "confidence":0.8,"ambiguous":true,"ambiguityReason":"Quantity not specified"}]}
                ```
                """);

        assertEquals(1, requests.size());
        assertEquals("Rahul", requests.getFirst().person());
        assertEquals(null, requests.getFirst().quantity());
        assertEquals(true, requests.getFirst().ambiguous());
    }

    @Test
    void rejectsMalformedGemmaJson() {
        assertThrows(InvalidAiResponseException.class, () -> parser.parse("{not valid"));
    }
}
