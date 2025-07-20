package com.claudecli.adapter.core;

import com.claudecli.adapter.model.ClaudeCliOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Component
public class ProcessExecutor {
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    public ProcessResult execute(List<String> command, ClaudeCliOptions options) {
        try {
            ProcessBuilder pb = createProcessBuilder(command, options);
            Process process = pb.start();
            
            CompletableFuture<String> outputFuture = readStream(process.getInputStream());
            CompletableFuture<String> errorFuture = readStream(process.getErrorStream());
            
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ProcessResult.builder()
                    .exitCode(-1)
                    .error("Process timeout after 5 minutes")
                    .timedOut(true)
                    .build();
            }
            
            return ProcessResult.builder()
                .exitCode(process.exitValue())
                .output(outputFuture.get(5, TimeUnit.SECONDS))
                .error(errorFuture.get(5, TimeUnit.SECONDS))
                .build();
                
        } catch (Exception e) {
            log.error("Failed to execute process", e);
            return ProcessResult.builder()
                .exitCode(-1)
                .error("Process execution failed: " + e.getMessage())
                .build();
        }
    }
    
    public void executeStream(List<String> command, ClaudeCliOptions options, 
                            Consumer<String> outputConsumer, Consumer<String> errorConsumer) {
        try {
            ProcessBuilder pb = createProcessBuilder(command, options);
            Process process = pb.start();
            
            streamOutput(process.getInputStream(), outputConsumer);
            streamOutput(process.getErrorStream(), errorConsumer);
            
            process.waitFor();
            
        } catch (Exception e) {
            log.error("Failed to execute streaming process", e);
            errorConsumer.accept("Process execution failed: " + e.getMessage());
        }
    }
    
    public CompletableFuture<ProcessResult> executeAsync(List<String> command, ClaudeCliOptions options) {
        return CompletableFuture.supplyAsync(() -> execute(command, options), executorService);
    }
    
    private ProcessBuilder createProcessBuilder(List<String> command, ClaudeCliOptions options) {
        ProcessBuilder pb = new ProcessBuilder(command);
        
        if (options.getWorkingDirectory() != null) {
            pb.directory(new File(options.getWorkingDirectory()));
        }
        
        if (options.getEnvironmentVariables() != null) {
            Map<String, String> env = pb.environment();
            env.putAll(options.getEnvironmentVariables());
        }
        
        return pb;
    }
    
    private CompletableFuture<String> readStream(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (IOException e) {
                return "Error reading stream: " + e.getMessage();
            }
        }, executorService);
    }
    
    private void streamOutput(InputStream inputStream, Consumer<String> consumer) {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    consumer.accept(line);
                }
            } catch (IOException e) {
                consumer.accept("Error reading stream: " + e.getMessage());
            }
        });
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String error;
        private final boolean timedOut;
    }
}