# Spring Claude CLI Adapter

A Spring Boot compatible library that wraps Claude CLI as a Spring Bean, enabling programmatic control and automation of Claude AI's conversational interface within Spring applications.

## 🚀 Key Features

- ✅ **Spring Bean Integration**: Easy usage through dependency injection
- ✅ **Multiple Execution Modes**: Support for synchronous, asynchronous, and streaming execution
- ✅ **Session Management**: Maintain conversation context with isolated multi-session support
- ✅ **Tmux Integration**: Advanced terminal control and background execution
- ✅ **Security Policies**: Command whitelisting/blacklisting and approval policies
- ✅ **Full CLI Options Support**: All Claude CLI flags available
- ✅ **Spring Boot Auto-configuration**: Instant setup with simple configuration
- ✅ **Real-time Stream Processing**: Process Claude responses in real-time

## 📋 Requirements

- Java 17 or higher
- Spring Boot 3.2.0 or higher
- Claude CLI installed on the system
- (Optional) tmux - for advanced session management features

## 🔧 Installation

### Maven

```xml
<dependency>
    <groupId>com.claudecli</groupId>
    <artifactId>spring-claude-cli-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.claudecli:spring-claude-cli-adapter:1.0.0-SNAPSHOT'
```

## 🎯 Quick Start

### 1. Basic Usage

```java
@Service
@RequiredArgsConstructor
public class ClaudeService {
    
    private final ClaudeCliWrapper claudeCli;
    
    public void basicExample() {
        // Simple query
        ClaudeResponse response = claudeCli.execute("What is Spring Boot?");
        System.out.println(response.getResponse());
    }
}
```

### 2. With Options

```java
public void advancedExample() {
    ClaudeCliOptions options = ClaudeCliOptions.builder()
        .model("claude-3-opus-20240229")
        .maxTokens(2000)
        .temperature(0.7)
        .dangerouslySkipPermissions(true)
        .workingDirectory("/my/project")
        .build();
    
    ClaudeResponse response = claudeCli.execute("Review this code", options);
}
```

### 3. Stream Processing

```java
public void streamExample() {
    // Receive responses in real-time
    claudeCli.executeStream("Write a long story", 
        line -> System.out.println("Claude: " + line));
}
```

### 4. Session Management

```java
public void sessionExample() {
    // Create a session that maintains conversation context
    ClaudeSession session = claudeCli.createSession("user-123");
    
    ClaudeResponse resp1 = session.send("My name is John");
    ClaudeResponse resp2 = session.send("What did I just tell you my name was?");
    // Claude remembers the previous conversation
    
    session.close();
}
```

### 5. Asynchronous Execution

```java
public void asyncExample() {
    CompletableFuture<ClaudeResponse> future = 
        claudeCli.executeAsync("Perform a complex task");
    
    future.thenAccept(response -> {
        System.out.println("Completed: " + response.getResponse());
    });
}
```

## ⚙️ Configuration

### application.yml

```yaml
claude:
  cli:
    # Path to Claude CLI executable
    cli-path: claude
    
    # Default model configuration
    default-model: claude-3-opus-20240229
    
    # API key (environment variable recommended)
    api-key: ${CLAUDE_API_KEY}
    
    # Auto-approve dangerous operations
    dangerously-skip-permissions: false
    
    # Working directory
    working-directory: ${user.dir}
    
    # Default parameters
    default-max-tokens: 4096
    default-temperature: 0.7
    
    # Session configuration
    session:
      persist-history: true
      persist-context: true
      history-directory: /tmp/claude-history
      context-directory: /tmp/claude-context
      max-sessions-per-user: 10
      
    # Security configuration
    security:
      enabled: true
      require-approval-for-all-commands: false
      log-all-commands: true
      whitelisted-commands:
        - ls
        - pwd
        - echo
        - cat
        - grep
        - git status
        - npm list
      blacklisted-commands:
        - rm -rf /
        - sudo rm
        - shutdown
      whitelisted-paths:
        - ${user.home}/Documents
        - ${user.home}/Downloads
        - /tmp
      blacklisted-paths:
        - /etc
        - /sys
        - /boot
        
    # Tmux configuration
    tmux:
      enabled: true
      default-session-prefix: claude-
      auto-cleanup-on-shutdown: true
```

## 🛡️ Security

### Custom Security Policy Implementation

```java
@Component
public class MySecurityPolicy implements CommandSecurityPolicy {
    
    @Override
    public boolean isCommandAllowed(String command) {
        // Block dangerous commands
        if (command.contains("rm -rf") || command.contains("format")) {
            return false;
        }
        return true;
    }
    
    @Override
    public ApprovalResult requiresApproval(CommandExecution cmd) {
        // Sudo commands require user approval
        if (cmd.getCommand().contains("sudo")) {
            return ApprovalResult.REQUIRES_USER_APPROVAL;
        }
        // Auto-approve file writes
        if (cmd.getCommand().startsWith("echo") && cmd.getCommand().contains(">")) {
            return ApprovalResult.APPROVED;
        }
        return ApprovalResult.APPROVED;
    }
    
    @Override
    public boolean isFileOperationAllowed(String filePath, FileOperation op) {
        Path path = Paths.get(filePath).normalize().toAbsolutePath();
        
        // Prevent writes to system directories
        if (op == FileOperation.WRITE || op == FileOperation.DELETE) {
            return !path.startsWith("/etc") && !path.startsWith("/sys");
        }
        return true;
    }
}
```

## 🎮 Advanced Features

### Tmux Session Management

```java
@Service
@RequiredArgsConstructor
public class TmuxClaudeService {
    
    private final ClaudeCliWrapper claudeCli;
    private final TmuxSessionManager tmuxManager;
    
    public void runInTmux() {
        // Run Claude in a tmux session
        ClaudeCliOptions options = ClaudeCliOptions.builder()
            .executionMode(ExecutionMode.TMUX)
            .tmuxOptions(TmuxOptions.builder()
                .sessionName("claude-dev")
                .windowName("ai-assistant")
                .detached(true)
                .logFile("/var/log/claude-session.log")
                .build())
            .build();
        
        claudeCli.execute("Start the development server", options);
        
        // Check session output later
        String output = tmuxManager.capturePane("claude-dev");
        System.out.println("Session output: " + output);
    }
}
```

### Parallel Execution

```java
public void parallelExecution() {
    List<String> prompts = Arrays.asList(
        "Write Python code",
        "Write Java code",
        "Write JavaScript code"
    );
    
    List<CompletableFuture<ClaudeResponse>> futures = prompts.stream()
        .map(prompt -> claudeCli.executeAsync(prompt))
        .collect(Collectors.toList());
    
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> {
            futures.forEach(f -> {
                try {
                    ClaudeResponse resp = f.get();
                    System.out.println(resp.getResponse());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
}
```

### REST API Integration Example

```java
@RestController
@RequestMapping("/api/claude")
@RequiredArgsConstructor
public class ClaudeController {
    
    private final ClaudeCliWrapper claudeCli;
    
    @PostMapping("/chat")
    public ClaudeResponse chat(@RequestBody ChatRequest request) {
        return claudeCli.execute(request.getMessage());
    }
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String prompt) {
        return Flux.create(sink -> {
            claudeCli.executeStream(prompt, sink::next);
            sink.complete();
        });
    }
    
    @PostMapping("/session/{userId}/message")
    public ClaudeResponse sessionMessage(
            @PathVariable String userId,
            @RequestBody String message) {
        
        ClaudeSession session = claudeCli.createSession(userId);
        return session.send(message);
    }
}
```

## 📁 Project Structure

```
spring-claude-cli-adapter/
├── src/main/java/com/claudecli/adapter/
│   ├── core/                    # Core interfaces and execution engine
│   ├── service/                 # Service implementations
│   ├── model/                   # Data models
│   ├── security/                # Security policies
│   ├── config/                  # Spring Boot auto-configuration
│   └── util/                    # Utilities
├── examples/                    # Example applications
└── pom.xml
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is distributed under the Apache License 2.0.

## 🔗 Related Links

- [Claude CLI Official Documentation](https://docs.anthropic.com/claude-cli)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Example Project](./examples/demo-app)

## ❓ FAQ

**Q: How do I install Claude CLI?**
A: Please refer to the [Claude CLI Installation Guide](https://docs.anthropic.com/claude-cli/installation).

**Q: Where can I get an API key?**
A: You can obtain an API key from the [Anthropic Console](https://console.anthropic.com).

**Q: Can multiple users use this concurrently?**
A: Yes, the session management feature maintains independent conversation contexts for each user.

**Q: Is this production-ready?**
A: Yes, with proper security policies configured and API usage monitoring in place.