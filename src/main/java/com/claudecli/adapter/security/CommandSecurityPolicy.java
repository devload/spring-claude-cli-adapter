package com.claudecli.adapter.security;

import com.claudecli.adapter.model.ClaudeResponse;
import java.util.List;

public interface CommandSecurityPolicy {
    
    boolean isCommandAllowed(String command);
    
    boolean isFileOperationAllowed(String filePath, FileOperation operation);
    
    ApprovalResult requiresApproval(ClaudeResponse.CommandExecution commandExecution);
    
    List<String> getWhitelistedCommands();
    
    List<String> getBlacklistedCommands();
    
    List<String> getWhitelistedPaths();
    
    List<String> getBlacklistedPaths();
    
    enum FileOperation {
        READ,
        WRITE,
        DELETE,
        EXECUTE
    }
    
    enum ApprovalResult {
        APPROVED,
        DENIED,
        REQUIRES_USER_APPROVAL
    }
}