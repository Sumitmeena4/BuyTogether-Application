package com.hackathon.gemma_app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProductNormalizationServiceTest {
    private final ProductNormalizationService service = new ProductNormalizationService();

    @Test
    void normalizesCaseAndSimplePluralsWithoutMergingDistinctProducts() {
        assertEquals("Blue Pen", service.normalize("blue pen"));
        assertEquals("Blue Pen", service.normalize("BLUE PENS"));
        assertEquals("Spiral Notebook", service.normalize("spiral notebooks"));
        assertEquals("Pen", service.normalize("pen"));
        assertEquals("Marker", service.normalize("markers"));
        assertEquals("Red Pen", service.normalize("red pens"));
        assertEquals("Blue Pen", service.normalize("blue pens."));
        assertEquals("Swiss Pen", service.normalize("swiss pens"));
        assertEquals("Status Light", service.normalize("status lights"));
        assertEquals("Glass", service.normalize("glasses"));
        assertEquals("Class", service.normalize("classes"));
    }
}
