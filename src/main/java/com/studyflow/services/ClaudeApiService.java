package com.studyflow.services;

import java.util.ArrayList;
import java.util.List;

public class ClaudeApiService {
    private static final String DEFAULT_MODEL = "openrouter/free";

    private final OpenRouterService openRouterService;
    private final String model;

    public ClaudeApiService() {
        this.openRouterService = new OpenRouterService();
        this.model = firstNonBlank(
                System.getProperty("openrouter.model"),
                System.getenv("OPENROUTER_MODEL"),
                DEFAULT_MODEL
        );
    }

    public boolean isConfigured() {
        return openRouterService.isConfigured();
    }

    public String ask(String prompt, String systemPrompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenRouter API key is not configured.");
        }

        List<OpenRouterService.ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new OpenRouterService.ChatMessage("system", systemPrompt));
        }
        messages.add(new OpenRouterService.ChatMessage("user", prompt == null ? "" : prompt));

        String response = openRouterService.chat(messages, model, 0.3, 1500);
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("OpenRouter returned an empty response.");
        }
        return response.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
