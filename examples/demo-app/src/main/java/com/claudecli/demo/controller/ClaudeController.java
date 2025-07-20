package com.claudecli.demo.controller;

import com.claudecli.adapter.core.ClaudeCliWrapper;
import com.claudecli.adapter.model.ClaudeCliOptions;
import com.claudecli.adapter.model.ClaudeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/claude")
@RequiredArgsConstructor
public class ClaudeController {
    
    private final ClaudeCliWrapper claudeCli;
    
    @PostMapping("/ask")
    public ClaudeResponse ask(@RequestBody AskRequest request) {
        log.info("Received request: {}", request.getPrompt());
        
        ClaudeCliOptions options = ClaudeCliOptions.builder()
            .model(request.getModel())
            .maxTokens(request.getMaxTokens())
            .temperature(request.getTemperature())
            .build();
        
        return claudeCli.execute(request.getPrompt(), options);
    }
    
    @PostMapping("/ask-async")
    public CompletableFuture<ClaudeResponse> askAsync(@RequestBody AskRequest request) {
        log.info("Received async request: {}", request.getPrompt());
        return claudeCli.executeAsync(request.getPrompt());
    }
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String prompt) {
        log.info("Streaming response for: {}", prompt);
        
        return Flux.create(sink -> {
            claudeCli.executeStream(prompt, line -> {
                sink.next(line);
            });
            sink.complete();
        }).delayElements(Duration.ofMillis(100));
    }
    
    @PostMapping("/session/{sessionId}/send")
    public ClaudeResponse sendToSession(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        
        ClaudeCliWrapper.ClaudeSession session = claudeCli.createSession(sessionId);
        return session.send(request.get("prompt"));
    }
    
    @DeleteMapping("/session/{sessionId}")
    public void closeSession(@PathVariable String sessionId) {
        claudeCli.destroySession(sessionId);
        log.info("Closed session: {}", sessionId);
    }
    
    @Data
    public static class AskRequest {
        private String prompt;
        private String model;
        private Integer maxTokens;
        private Double temperature;
    }
}