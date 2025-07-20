package com.claudecli.adapter.core;

import com.claudecli.adapter.model.ClaudeCliOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClaudeCliCommandBuilder {
    
    private static final String CLAUDE_CLI = "claude";
    
    public List<String> buildCommand(String prompt, ClaudeCliOptions options) {
        List<String> command = new ArrayList<>();
        
        if (options.getExecutionMode() == ClaudeCliOptions.ExecutionMode.TMUX) {
            buildTmuxPrefix(command, options.getTmuxOptions());
        }
        
        command.add(CLAUDE_CLI);
        
        if (options.getModel() != null) {
            command.add("--model");
            command.add(options.getModel());
        }
        
        if (options.getOutputFormat() != null) {
            command.add("--output-format");
            command.add(options.getOutputFormat());
        }
        
        if (options.getApiKey() != null) {
            command.add("--api-key");
            command.add(options.getApiKey());
        }
        
        if (options.getApiUrl() != null) {
            command.add("--api-url");
            command.add(options.getApiUrl());
        }
        
        if (Boolean.TRUE.equals(options.getDangerouslySkipPermissions())) {
            command.add("--dangerously-skip-permissions");
        }
        
        if (Boolean.TRUE.equals(options.getContinueMode())) {
            command.add("--continue");
        }
        
        if (Boolean.TRUE.equals(options.getVerbose())) {
            command.add("--verbose");
        }
        
        if (options.getContextFile() != null) {
            command.add("--context");
            command.add(options.getContextFile());
        }
        
        if (options.getHistoryFile() != null) {
            command.add("--history");
            command.add(options.getHistoryFile());
        }
        
        if (options.getOutputFile() != null) {
            command.add("--output");
            command.add(options.getOutputFile());
        }
        
        if (options.getMaxTokens() != null) {
            command.add("--max-tokens");
            command.add(options.getMaxTokens().toString());
        }
        
        if (options.getTemperature() != null) {
            command.add("--temperature");
            command.add(options.getTemperature().toString());
        }
        
        if (options.getAdditionalFlags() != null) {
            command.addAll(options.getAdditionalFlags());
        }
        
        if (prompt != null && !prompt.isEmpty()) {
            command.add("--");
            command.add(prompt);
        }
        
        return command;
    }
    
    private void buildTmuxPrefix(List<String> command, ClaudeCliOptions.TmuxOptions tmuxOptions) {
        command.add("tmux");
        
        if (tmuxOptions.getSessionName() != null) {
            command.add("new-session");
            command.add("-s");
            command.add(tmuxOptions.getSessionName());
        } else {
            command.add("new-session");
        }
        
        if (tmuxOptions.getWindowName() != null) {
            command.add("-n");
            command.add(tmuxOptions.getWindowName());
        }
        
        if (Boolean.TRUE.equals(tmuxOptions.getDetached())) {
            command.add("-d");
        }
        
        command.add("--");
    }
}