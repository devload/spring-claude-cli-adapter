package com.claudecli.adapter.security;

import com.claudecli.adapter.model.ClaudeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@ConfigurationProperties(prefix = "claude.cli.security")
public class DefaultCommandSecurityPolicy implements CommandSecurityPolicy {
    
    private List<String> whitelistedCommands = List.of(
        "ls", "pwd", "echo", "cat", "grep", "find", "which", "date", "whoami"
    );
    
    private List<String> blacklistedCommands = List.of(
        "rm -rf /", "dd", "mkfs", "format", ":(){:|:&};:", "shutdown", "reboot"
    );
    
    private List<Pattern> whitelistedCommandPatterns = List.of(
        Pattern.compile("^git (status|log|diff|show).*"),
        Pattern.compile("^npm (list|info|view).*"),
        Pattern.compile("^yarn (list|info|why).*")
    );
    
    private List<Pattern> blacklistedCommandPatterns = List.of(
        Pattern.compile(".*\\brm\\s+-rf\\s+/.*"),
        Pattern.compile(".*\\bsudo\\s+rm.*"),
        Pattern.compile(".*\\b(curl|wget).*\\|.*sh.*")
    );
    
    private List<String> whitelistedPaths = List.of(
        System.getProperty("user.home") + "/Documents",
        System.getProperty("user.home") + "/Downloads",
        "/tmp",
        "/var/tmp"
    );
    
    private List<String> blacklistedPaths = List.of(
        "/",
        "/etc",
        "/usr",
        "/bin",
        "/sbin",
        "/boot",
        "/sys",
        "/proc"
    );
    
    private boolean requireApprovalForAllCommands = false;
    private boolean logAllCommands = true;
    
    @Override
    public boolean isCommandAllowed(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        
        String trimmedCommand = command.trim().toLowerCase();
        
        // Check blacklist first
        if (blacklistedCommands.stream().anyMatch(trimmedCommand::startsWith)) {
            log.warn("Command blocked by blacklist: {}", command);
            return false;
        }
        
        if (blacklistedCommandPatterns.stream().anyMatch(p -> p.matcher(command).matches())) {
            log.warn("Command blocked by blacklist pattern: {}", command);
            return false;
        }
        
        // Check whitelist
        if (whitelistedCommands.stream().anyMatch(trimmedCommand::startsWith)) {
            log.debug("Command allowed by whitelist: {}", command);
            return true;
        }
        
        if (whitelistedCommandPatterns.stream().anyMatch(p -> p.matcher(command).matches())) {
            log.debug("Command allowed by whitelist pattern: {}", command);
            return true;
        }
        
        // Default deny
        log.warn("Command not explicitly allowed: {}", command);
        return false;
    }
    
    @Override
    public boolean isFileOperationAllowed(String filePath, FileOperation operation) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path path = Paths.get(filePath).normalize().toAbsolutePath();
            String pathStr = path.toString();
            
            // Check blacklisted paths
            if (blacklistedPaths.stream().anyMatch(pathStr::startsWith)) {
                log.warn("File operation blocked for blacklisted path: {} ({})", filePath, operation);
                return false;
            }
            
            // For write/delete operations, check whitelist
            if (operation == FileOperation.WRITE || operation == FileOperation.DELETE) {
                boolean allowed = whitelistedPaths.stream()
                    .anyMatch(whitePath -> pathStr.startsWith(whitePath));
                
                if (!allowed) {
                    log.warn("Write/Delete operation not allowed outside whitelisted paths: {} ({})", 
                        filePath, operation);
                }
                
                return allowed;
            }
            
            // Read operations are generally allowed outside blacklisted paths
            return true;
            
        } catch (Exception e) {
            log.error("Error checking file operation permission", e);
            return false;
        }
    }
    
    @Override
    public ApprovalResult requiresApproval(ClaudeResponse.CommandExecution commandExecution) {
        if (commandExecution == null || commandExecution.getCommand() == null) {
            return ApprovalResult.DENIED;
        }
        
        if (requireApprovalForAllCommands) {
            return ApprovalResult.REQUIRES_USER_APPROVAL;
        }
        
        if (!isCommandAllowed(commandExecution.getCommand())) {
            return ApprovalResult.DENIED;
        }
        
        // Check for potentially dangerous patterns that require approval
        String command = commandExecution.getCommand().toLowerCase();
        if (command.contains("sudo") || 
            command.contains("chmod") || 
            command.contains("chown") ||
            command.contains(">") ||
            command.contains(">>")) {
            return ApprovalResult.REQUIRES_USER_APPROVAL;
        }
        
        return ApprovalResult.APPROVED;
    }
    
    @Override
    public List<String> getWhitelistedCommands() {
        return whitelistedCommands;
    }
    
    @Override
    public List<String> getBlacklistedCommands() {
        return blacklistedCommands;
    }
    
    @Override
    public List<String> getWhitelistedPaths() {
        return whitelistedPaths;
    }
    
    @Override
    public List<String> getBlacklistedPaths() {
        return blacklistedPaths;
    }
    
    public void setWhitelistedCommands(List<String> whitelistedCommands) {
        this.whitelistedCommands = whitelistedCommands;
    }
    
    public void setBlacklistedCommands(List<String> blacklistedCommands) {
        this.blacklistedCommands = blacklistedCommands;
    }
    
    public void setWhitelistedPaths(List<String> whitelistedPaths) {
        this.whitelistedPaths = whitelistedPaths;
    }
    
    public void setBlacklistedPaths(List<String> blacklistedPaths) {
        this.blacklistedPaths = blacklistedPaths;
    }
    
    public void setRequireApprovalForAllCommands(boolean requireApprovalForAllCommands) {
        this.requireApprovalForAllCommands = requireApprovalForAllCommands;
    }
    
    public void setLogAllCommands(boolean logAllCommands) {
        this.logAllCommands = logAllCommands;
    }
}