package com.claudecli.adapter.service;

import com.claudecli.adapter.core.ProcessExecutor;
import com.claudecli.adapter.model.ClaudeCliOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmuxSessionManager {
    
    private final ProcessExecutor processExecutor;
    private final Map<String, TmuxSession> activeSessions = new ConcurrentHashMap<>();
    
    public TmuxSession createSession(String sessionName, ClaudeCliOptions.TmuxOptions options) {
        if (sessionExists(sessionName)) {
            log.warn("Tmux session {} already exists", sessionName);
            return activeSessions.get(sessionName);
        }
        
        List<String> createCommand = Arrays.asList(
            "tmux", "new-session", "-d", "-s", sessionName
        );
        
        if (options.getWindowName() != null) {
            createCommand = Arrays.asList(
                "tmux", "new-session", "-d", "-s", sessionName, "-n", options.getWindowName()
            );
        }
        
        ProcessExecutor.ProcessResult result = processExecutor.execute(createCommand, 
            ClaudeCliOptions.builder().build());
        
        if (result.getExitCode() == 0) {
            TmuxSession session = new TmuxSession(sessionName, options);
            activeSessions.put(sessionName, session);
            log.info("Created tmux session: {}", sessionName);
            return session;
        } else {
            throw new RuntimeException("Failed to create tmux session: " + result.getError());
        }
    }
    
    public void sendCommand(String sessionName, String command) {
        if (!sessionExists(sessionName)) {
            throw new IllegalArgumentException("Tmux session does not exist: " + sessionName);
        }
        
        List<String> sendCommand = Arrays.asList(
            "tmux", "send-keys", "-t", sessionName, command, "Enter"
        );
        
        ProcessExecutor.ProcessResult result = processExecutor.execute(sendCommand, 
            ClaudeCliOptions.builder().build());
        
        if (result.getExitCode() != 0) {
            log.error("Failed to send command to tmux session {}: {}", sessionName, result.getError());
        }
    }
    
    public String capturePane(String sessionName) {
        if (!sessionExists(sessionName)) {
            throw new IllegalArgumentException("Tmux session does not exist: " + sessionName);
        }
        
        List<String> captureCommand = Arrays.asList(
            "tmux", "capture-pane", "-t", sessionName, "-p"
        );
        
        ProcessExecutor.ProcessResult result = processExecutor.execute(captureCommand, 
            ClaudeCliOptions.builder().build());
        
        if (result.getExitCode() == 0) {
            return result.getOutput();
        } else {
            log.error("Failed to capture pane from tmux session {}: {}", sessionName, result.getError());
            return "";
        }
    }
    
    public void attachSession(String sessionName) {
        if (!sessionExists(sessionName)) {
            throw new IllegalArgumentException("Tmux session does not exist: " + sessionName);
        }
        
        List<String> attachCommand = Arrays.asList(
            "tmux", "attach-session", "-t", sessionName
        );
        
        processExecutor.execute(attachCommand, ClaudeCliOptions.builder().build());
    }
    
    public void killSession(String sessionName) {
        if (!sessionExists(sessionName)) {
            return;
        }
        
        List<String> killCommand = Arrays.asList(
            "tmux", "kill-session", "-t", sessionName
        );
        
        ProcessExecutor.ProcessResult result = processExecutor.execute(killCommand, 
            ClaudeCliOptions.builder().build());
        
        if (result.getExitCode() == 0) {
            activeSessions.remove(sessionName);
            log.info("Killed tmux session: {}", sessionName);
        } else {
            log.error("Failed to kill tmux session {}: {}", sessionName, result.getError());
        }
    }
    
    public boolean sessionExists(String sessionName) {
        List<String> checkCommand = Arrays.asList(
            "tmux", "has-session", "-t", sessionName
        );
        
        ProcessExecutor.ProcessResult result = processExecutor.execute(checkCommand, 
            ClaudeCliOptions.builder().build());
        
        return result.getExitCode() == 0;
    }
    
    public List<String> listSessions() {
        List<String> listCommand = Arrays.asList(
            "tmux", "list-sessions", "-F", "#{session_name}"
        );
        
        ProcessExecutor.ProcessResult result = processExecutor.execute(listCommand, 
            ClaudeCliOptions.builder().build());
        
        if (result.getExitCode() == 0) {
            return Arrays.asList(result.getOutput().split("\n"));
        } else {
            return List.of();
        }
    }
    
    public void killAllSessions() {
        activeSessions.keySet().forEach(this::killSession);
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TmuxSession {
        private final String sessionName;
        private final ClaudeCliOptions.TmuxOptions options;
    }
}