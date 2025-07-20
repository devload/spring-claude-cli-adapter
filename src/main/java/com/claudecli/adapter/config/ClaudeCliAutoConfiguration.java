package com.claudecli.adapter.config;

import com.claudecli.adapter.core.ClaudeCliCommandBuilder;
import com.claudecli.adapter.core.ClaudeCliWrapper;
import com.claudecli.adapter.core.ProcessExecutor;
import com.claudecli.adapter.security.CommandSecurityPolicy;
import com.claudecli.adapter.security.DefaultCommandSecurityPolicy;
import com.claudecli.adapter.service.ClaudeCliService;
import com.claudecli.adapter.service.TmuxSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ConditionalOnClass(ClaudeCliWrapper.class)
@EnableConfigurationProperties(ClaudeCliProperties.class)
@ComponentScan(basePackages = "com.claudecli.adapter")
public class ClaudeCliAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessExecutor processExecutor() {
        return new ProcessExecutor();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ClaudeCliCommandBuilder claudeCliCommandBuilder() {
        return new ClaudeCliCommandBuilder();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CommandSecurityPolicy commandSecurityPolicy() {
        return new DefaultCommandSecurityPolicy();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "claude.cli.tmux",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public TmuxSessionManager tmuxSessionManager(ProcessExecutor processExecutor) {
        return new TmuxSessionManager(processExecutor);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ClaudeCliWrapper claudeCliWrapper(
            ProcessExecutor processExecutor,
            ClaudeCliCommandBuilder commandBuilder,
            CommandSecurityPolicy securityPolicy,
            ObjectMapper objectMapper) {
        return new ClaudeCliService(processExecutor, commandBuilder, securityPolicy, objectMapper);
    }
}