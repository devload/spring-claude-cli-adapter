package com.claudecli.adapter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "claude.cli")
public class ClaudeCliProperties {
    
    private String cliPath = "claude";
    private String defaultModel = "claude-3-opus-20240229";
    private String defaultOutputFormat = "text";
    private String apiKey;
    private String apiUrl;
    
    private Boolean dangerouslySkipPermissions = false;
    private Boolean verbose = false;
    
    private String workingDirectory = System.getProperty("user.dir");
    private String sessionDirectory = "/tmp/claude-sessions";
    
    private Integer defaultMaxTokens = 4096;
    private Double defaultTemperature = 0.7;
    
    private Map<String, String> defaultEnvironmentVariables;
    
    private SessionConfig session = new SessionConfig();
    private SecurityConfig security = new SecurityConfig();
    private TmuxConfig tmux = new TmuxConfig();
    
    @Data
    public static class SessionConfig {
        private Boolean persistHistory = true;
        private Boolean persistContext = true;
        private String historyDirectory = "/tmp/claude-history";
        private String contextDirectory = "/tmp/claude-context";
        private Integer maxSessionsPerUser = 10;
    }
    
    @Data
    public static class SecurityConfig {
        private Boolean enabled = true;
        private Boolean requireApprovalForAllCommands = false;
        private Boolean logAllCommands = true;
        private String approvalWebhookUrl;
    }
    
    @Data
    public static class TmuxConfig {
        private Boolean enabled = true;
        private String defaultSessionPrefix = "claude-";
        private Boolean autoCleanupOnShutdown = true;
    }
}