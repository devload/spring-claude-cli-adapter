package com.claudecli.adapter.core;

import com.claudecli.adapter.model.ClaudeCliOptions;
import com.claudecli.adapter.model.ClaudeResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ClaudeCliWrapper {
    
    ClaudeResponse execute(String prompt);
    
    ClaudeResponse execute(String prompt, ClaudeCliOptions options);
    
    CompletableFuture<ClaudeResponse> executeAsync(String prompt);
    
    CompletableFuture<ClaudeResponse> executeAsync(String prompt, ClaudeCliOptions options);
    
    void executeStream(String prompt, Consumer<String> streamConsumer);
    
    void executeStream(String prompt, ClaudeCliOptions options, Consumer<String> streamConsumer);
    
    ClaudeSession createSession(String sessionId);
    
    ClaudeSession createSession(String sessionId, ClaudeCliOptions defaultOptions);
    
    void destroySession(String sessionId);
    
    boolean isSessionActive(String sessionId);
    
    interface ClaudeSession {
        
        String getSessionId();
        
        ClaudeResponse send(String prompt);
        
        CompletableFuture<ClaudeResponse> sendAsync(String prompt);
        
        void sendStream(String prompt, Consumer<String> streamConsumer);
        
        void close();
        
        boolean isActive();
        
        ClaudeCliOptions getDefaultOptions();
        
        void updateDefaultOptions(ClaudeCliOptions options);
    }
}