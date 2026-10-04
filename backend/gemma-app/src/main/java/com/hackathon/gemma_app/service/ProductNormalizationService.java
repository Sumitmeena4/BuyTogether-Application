package com.hackathon.gemma_app.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProductNormalizationService {
    public String normalize(String product) {
        if (product == null || product.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank.");
        }
        String[] words = product.trim().replaceAll("\\s+", " ").split(" ");
        words[words.length - 1] = words[words.length - 1].replaceAll("[,.;:!?]+$", "");
        if (words[words.length - 1].isBlank()) {
            throw new IllegalArgumentException("Product name cannot end with punctuation only.");
        }
        words[words.length - 1] = singularize(words[words.length - 1]);
        String normalized = Arrays.stream(words)
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT)
                        + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("Product name cannot exceed 200 characters.");
        }
        return normalized;
    }

    public String key(String product) {
        return normalize(product).toLowerCase(Locale.ROOT);
    }

    private String singularize(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        if (lower.length() > 4 && lower.endsWith("ies")) {
            return lower.substring(0, lower.length() - 3) + "y";
        }
        if (lower.length() > 4 && (lower.endsWith("ches") || lower.endsWith("shes")
                || lower.endsWith("xes") || lower.endsWith("zes") || lower.endsWith("sses"))) {
            return lower.substring(0, lower.length() - 2);
        }
        if (lower.length() > 3 && lower.endsWith("s")
                && !lower.endsWith("ss") && !lower.endsWith("us") && !lower.endsWith("is")) {
            return lower.substring(0, lower.length() - 1);
        }
        return lower;
    }
}
