package com.claudecli.adapter.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
@Builder
public class ClaudeCliOptions {
    
    private String prompt;
    private String model;
    private String outputFormat;
    private String apiKey;
    private String apiUrl;
    
    private Boolean dangerouslySkipPermissions;
    private Boolean continueMode;
    private Boolean verbose;
    private Boolean streamJson;
    
    private String contextFile;
    private String historyFile;
    private String outputFile;
    
    private Integer maxTokens;
    private Double temperature;
    
    private List<String> additionalFlags;
    private Map<String, String> environmentVariables;
    
    private String workingDirectory;
    
    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.DIRECT;
    
    private TmuxOptions tmuxOptions;
    
    public enum ExecutionMode {
        DIRECT,
        TMUX,
        PARALLEL
    }
    
    @Data
    @Builder
    public static class TmuxOptions {
        private String sessionName;
        private String windowName;
        private Boolean detached;
        private String logFile;
    }
}