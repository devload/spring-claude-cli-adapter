package com.claudecli.adapter.service;

import com.claudecli.adapter.core.ClaudeCliCommandBuilder;
import com.claudecli.adapter.core.ClaudeCliWrapper;
import com.claudecli.adapter.core.ProcessExecutor;
import com.claudecli.adapter.model.ClaudeCliOptions;
import com.claudecli.adapter.model.ClaudeResponse;
import com.claudecli.adapter.security.CommandSecurityPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeCliService implements ClaudeCliWrapper {
    
    private final ProcessExecutor processExecutor;
    private final ClaudeCliCommandBuilder commandBuilder;
    private final CommandSecurityPolicy securityPolicy;
    private final ObjectMapper objectMapper;
    
    private final Map<String, ClaudeSessionImpl> sessions = new ConcurrentHashMap<>();
    
    @Override
    public ClaudeResponse execute(String prompt) {
        return execute(prompt, ClaudeCliOptions.builder().build());
    }
    
    @Override
    public ClaudeResponse execute(String prompt, ClaudeCliOptions options) {
        List<String> command = commandBuilder.buildCommand(prompt, options);
        ProcessExecutor.ProcessResult result = processExecutor.execute(command, options);
        
        return buildResponse(prompt, result);
    }
    
    @Override
    public CompletableFuture<ClaudeResponse> executeAsync(String prompt) {
        return executeAsync(prompt, ClaudeCliOptions.builder().build());
    }
    
    @Override
    public CompletableFuture<ClaudeResponse> executeAsync(String prompt, ClaudeCliOptions options) {
        List<String> command = commandBuilder.buildCommand(prompt, options);
        return processExecutor.executeAsync(command, options)
            .thenApply(result -> buildResponse(prompt, result));
    }
    
    @Override
    public void executeStream(String prompt, Consumer<String> streamConsumer) {
        executeStream(prompt, ClaudeCliOptions.builder().build(), streamConsumer);
    }
    
    @Override
    public void executeStream(String prompt, ClaudeCliOptions options, Consumer<String> streamConsumer) {
        ClaudeCliOptions streamOptions = ClaudeCliOptions.builder()
            .outputFormat("stream-json")
            .build();
        
        if (options != null) {
            streamOptions = mergeOptions(options, streamOptions);
        }
        
        List<String> command = commandBuilder.buildCommand(prompt, streamOptions);
        
        StringBuilder responseBuilder = new StringBuilder();
        processExecutor.executeStream(command, streamOptions, 
            line -> {
                streamConsumer.accept(line);
                responseBuilder.append(line).append("\n");
            },
            error -> log.error("Stream error: {}", error)
        );
    }
    
    @Override
    public ClaudeSession createSession(String sessionId) {
        return createSession(sessionId, ClaudeCliOptions.builder().build());
    }
    
    @Override
    public ClaudeSession createSession(String sessionId, ClaudeCliOptions defaultOptions) {
        ClaudeSessionImpl session = new ClaudeSessionImpl(sessionId, defaultOptions);
        sessions.put(sessionId, session);
        return session;
    }
    
    @Override
    public void destroySession(String sessionId) {
        ClaudeSessionImpl session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }
    
    @Override
    public boolean isSessionActive(String sessionId) {
        ClaudeSessionImpl session = sessions.get(sessionId);
        return session != null && session.isActive();
    }
    
    @PreDestroy
    public void cleanup() {
        sessions.values().forEach(ClaudeSessionImpl::close);
        sessions.clear();
        processExecutor.shutdown();
    }
    
    private ClaudeResponse buildResponse(String prompt, ProcessExecutor.ProcessResult result) {
        ClaudeResponse.ResponseStatus status = result.getExitCode() == 0 
            ? ClaudeResponse.ResponseStatus.SUCCESS 
            : ClaudeResponse.ResponseStatus.ERROR;
        
        if (result.isTimedOut()) {
            status = ClaudeResponse.ResponseStatus.TIMEOUT;
        }
        
        return ClaudeResponse.builder()
            .sessionId(UUID.randomUUID().toString())
            .prompt(prompt)
            .response(result.getOutput())
            .status(status)
            .timestamp(LocalDateTime.now())
            .rawOutput(result.getOutput())
            .errorOutput(result.getError())
            .exitCode(result.getExitCode())
            .build();
    }
    
    private ClaudeCliOptions mergeOptions(ClaudeCliOptions base, ClaudeCliOptions overlay) {
        return ClaudeCliOptions.builder()
            .prompt(overlay.getPrompt() != null ? overlay.getPrompt() : base.getPrompt())
            .model(overlay.getModel() != null ? overlay.getModel() : base.getModel())
            .outputFormat(overlay.getOutputFormat() != null ? overlay.getOutputFormat() : base.getOutputFormat())
            .apiKey(overlay.getApiKey() != null ? overlay.getApiKey() : base.getApiKey())
            .apiUrl(overlay.getApiUrl() != null ? overlay.getApiUrl() : base.getApiUrl())
            .dangerouslySkipPermissions(overlay.getDangerouslySkipPermissions() != null ? overlay.getDangerouslySkipPermissions() : base.getDangerouslySkipPermissions())
            .continueMode(overlay.getContinueMode() != null ? overlay.getContinueMode() : base.getContinueMode())
            .verbose(overlay.getVerbose() != null ? overlay.getVerbose() : base.getVerbose())
            .streamJson(overlay.getStreamJson() != null ? overlay.getStreamJson() : base.getStreamJson())
            .contextFile(overlay.getContextFile() != null ? overlay.getContextFile() : base.getContextFile())
            .historyFile(overlay.getHistoryFile() != null ? overlay.getHistoryFile() : base.getHistoryFile())
            .outputFile(overlay.getOutputFile() != null ? overlay.getOutputFile() : base.getOutputFile())
            .maxTokens(overlay.getMaxTokens() != null ? overlay.getMaxTokens() : base.getMaxTokens())
            .temperature(overlay.getTemperature() != null ? overlay.getTemperature() : base.getTemperature())
            .additionalFlags(overlay.getAdditionalFlags() != null ? overlay.getAdditionalFlags() : base.getAdditionalFlags())
            .environmentVariables(overlay.getEnvironmentVariables() != null ? overlay.getEnvironmentVariables() : base.getEnvironmentVariables())
            .workingDirectory(overlay.getWorkingDirectory() != null ? overlay.getWorkingDirectory() : base.getWorkingDirectory())
            .executionMode(overlay.getExecutionMode() != null ? overlay.getExecutionMode() : base.getExecutionMode())
            .tmuxOptions(overlay.getTmuxOptions() != null ? overlay.getTmuxOptions() : base.getTmuxOptions())
            .build();
    }
    
    @RequiredArgsConstructor
    private class ClaudeSessionImpl implements ClaudeSession {
        
        private final String sessionId;
        private ClaudeCliOptions defaultOptions;
        private volatile boolean active = true;
        
        @Override
        public String getSessionId() {
            return sessionId;
        }
        
        @Override
        public ClaudeResponse send(String prompt) {
            if (!active) {
                throw new IllegalStateException("Session is closed");
            }
            
            ClaudeCliOptions sessionOptions = ClaudeCliOptions.builder()
                .historyFile(getHistoryFile())
                .contextFile(getContextFile())
                .build();
            
            ClaudeCliOptions mergedOptions = mergeOptions(defaultOptions, sessionOptions);
            return execute(prompt, mergedOptions);
        }
        
        @Override
        public CompletableFuture<ClaudeResponse> sendAsync(String prompt) {
            if (!active) {
                throw new IllegalStateException("Session is closed");
            }
            
            ClaudeCliOptions sessionOptions = ClaudeCliOptions.builder()
                .historyFile(getHistoryFile())
                .contextFile(getContextFile())
                .build();
            
            ClaudeCliOptions mergedOptions = mergeOptions(defaultOptions, sessionOptions);
            return executeAsync(prompt, mergedOptions);
        }
        
        @Override
        public void sendStream(String prompt, Consumer<String> streamConsumer) {
            if (!active) {
                throw new IllegalStateException("Session is closed");
            }
            
            ClaudeCliOptions sessionOptions = ClaudeCliOptions.builder()
                .historyFile(getHistoryFile())
                .contextFile(getContextFile())
                .build();
            
            ClaudeCliOptions mergedOptions = mergeOptions(defaultOptions, sessionOptions);
            executeStream(prompt, mergedOptions, streamConsumer);
        }
        
        @Override
        public void close() {
            active = false;
            sessions.remove(sessionId);
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
        
        @Override
        public ClaudeCliOptions getDefaultOptions() {
            return defaultOptions;
        }
        
        @Override
        public void updateDefaultOptions(ClaudeCliOptions options) {
            this.defaultOptions = options;
        }
        
        private String getHistoryFile() {
            return "/tmp/claude-session-" + sessionId + ".history";
        }
        
        private String getContextFile() {
            return "/tmp/claude-session-" + sessionId + ".context";
        }
    }
}