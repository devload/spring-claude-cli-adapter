package com.claudecli.adapter.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClaudeResponse {
    
    private String sessionId;
    private String prompt;
    private String response;
    private ResponseStatus status;
    private LocalDateTime timestamp;
    
    private List<ToolCall> toolCalls;
    private List<FileChange> fileChanges;
    private List<CommandExecution> commandExecutions;
    
    private String rawOutput;
    private String errorOutput;
    private Integer exitCode;
    
    public enum ResponseStatus {
        SUCCESS,
        PARTIAL,
        ERROR,
        CANCELLED,
        TIMEOUT
    }
    
    @Data
    @Builder
    public static class ToolCall {
        private String toolName;
        private String action;
        private String result;
        private LocalDateTime timestamp;
    }
    
    @Data
    @Builder
    public static class FileChange {
        private String filePath;
        private ChangeType changeType;
        private String content;
        private LocalDateTime timestamp;
        
        public enum ChangeType {
            CREATE,
            MODIFY,
            DELETE
        }
    }
    
    @Data
    @Builder
    public static class CommandExecution {
        private String command;
        private String output;
        private String error;
        private Integer exitCode;
        private LocalDateTime timestamp;
        private Boolean approved;
    }
}