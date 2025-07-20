# Spring Claude CLI Adapter

Spring Boot 애플리케이션에서 Claude CLI를 Spring Bean으로 사용할 수 있게 해주는 라이브러리입니다. Claude AI와의 대화형 인터페이스를 프로그래밍 방식으로 제어하고 자동화할 수 있습니다.

## 🚀 주요 기능

- ✅ **Spring Bean 통합**: DI를 통한 간편한 사용
- ✅ **다양한 실행 모드**: 동기/비동기/스트림 실행 지원
- ✅ **세션 관리**: 대화 컨텍스트 유지 및 격리된 멀티 세션
- ✅ **Tmux 통합**: 고급 터미널 제어 및 백그라운드 실행
- ✅ **보안 정책**: 명령어 화이트리스트/블랙리스트, 승인 정책
- ✅ **완전한 CLI 옵션 지원**: 모든 Claude CLI 플래그 사용 가능
- ✅ **Spring Boot 자동 설정**: 간단한 설정으로 즉시 사용
- ✅ **실시간 스트림 처리**: Claude 응답을 실시간으로 처리

## 📋 요구사항

- Java 17 이상
- Spring Boot 3.2.0 이상
- Claude CLI가 시스템에 설치되어 있어야 함
- (선택사항) tmux - 고급 세션 관리 기능 사용 시

## 🔧 설치

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

## 🎯 빠른 시작

### 1. 기본 사용법

```java
@Service
@RequiredArgsConstructor
public class ClaudeService {
    
    private final ClaudeCliWrapper claudeCli;
    
    public void basicExample() {
        // 간단한 질문
        ClaudeResponse response = claudeCli.execute("Spring Boot가 뭔가요?");
        System.out.println(response.getResponse());
    }
}
```

### 2. 옵션 설정

```java
public void advancedExample() {
    ClaudeCliOptions options = ClaudeCliOptions.builder()
        .model("claude-3-opus-20240229")
        .maxTokens(2000)
        .temperature(0.7)
        .dangerouslySkipPermissions(true)
        .workingDirectory("/my/project")
        .build();
    
    ClaudeResponse response = claudeCli.execute("코드 리뷰해줘", options);
}
```

### 3. 스트림 처리

```java
public void streamExample() {
    // 실시간으로 응답 받기
    claudeCli.executeStream("긴 이야기를 써줘", 
        line -> System.out.println("Claude: " + line));
}
```

### 4. 세션 관리

```java
public void sessionExample() {
    // 대화 컨텍스트를 유지하는 세션 생성
    ClaudeSession session = claudeCli.createSession("user-123");
    
    ClaudeResponse resp1 = session.send("내 이름은 홍길동이야");
    ClaudeResponse resp2 = session.send("내 이름이 뭐라고 했지?");
    // Claude가 이전 대화 내용을 기억함
    
    session.close();
}
```

### 5. 비동기 실행

```java
public void asyncExample() {
    CompletableFuture<ClaudeResponse> future = 
        claudeCli.executeAsync("복잡한 작업 수행해줘");
    
    future.thenAccept(response -> {
        System.out.println("완료: " + response.getResponse());
    });
}
```

## ⚙️ 설정

### application.yml

```yaml
claude:
  cli:
    # Claude CLI 실행 파일 경로
    cli-path: claude
    
    # 기본 모델 설정
    default-model: claude-3-opus-20240229
    
    # API 키 (환경변수 권장)
    api-key: ${CLAUDE_API_KEY}
    
    # 위험한 작업 자동 승인
    dangerously-skip-permissions: false
    
    # 작업 디렉토리
    working-directory: ${user.dir}
    
    # 기본 파라미터
    default-max-tokens: 4096
    default-temperature: 0.7
    
    # 세션 설정
    session:
      persist-history: true
      persist-context: true
      history-directory: /tmp/claude-history
      context-directory: /tmp/claude-context
      max-sessions-per-user: 10
      
    # 보안 설정
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
        
    # Tmux 설정
    tmux:
      enabled: true
      default-session-prefix: claude-
      auto-cleanup-on-shutdown: true
```

## 🛡️ 보안

### 커스텀 보안 정책 구현

```java
@Component
public class MySecurityPolicy implements CommandSecurityPolicy {
    
    @Override
    public boolean isCommandAllowed(String command) {
        // 위험한 명령어 차단
        if (command.contains("rm -rf") || command.contains("format")) {
            return false;
        }
        return true;
    }
    
    @Override
    public ApprovalResult requiresApproval(CommandExecution cmd) {
        // sudo 명령어는 사용자 승인 필요
        if (cmd.getCommand().contains("sudo")) {
            return ApprovalResult.REQUIRES_USER_APPROVAL;
        }
        // 파일 쓰기는 자동 승인
        if (cmd.getCommand().startsWith("echo") && cmd.getCommand().contains(">")) {
            return ApprovalResult.APPROVED;
        }
        return ApprovalResult.APPROVED;
    }
    
    @Override
    public boolean isFileOperationAllowed(String filePath, FileOperation op) {
        Path path = Paths.get(filePath).normalize().toAbsolutePath();
        
        // 시스템 디렉토리 쓰기 금지
        if (op == FileOperation.WRITE || op == FileOperation.DELETE) {
            return !path.startsWith("/etc") && !path.startsWith("/sys");
        }
        return true;
    }
}
```

## 🎮 고급 기능

### Tmux 세션 관리

```java
@Service
@RequiredArgsConstructor
public class TmuxClaudeService {
    
    private final ClaudeCliWrapper claudeCli;
    private final TmuxSessionManager tmuxManager;
    
    public void runInTmux() {
        // Tmux 세션에서 Claude 실행
        ClaudeCliOptions options = ClaudeCliOptions.builder()
            .executionMode(ExecutionMode.TMUX)
            .tmuxOptions(TmuxOptions.builder()
                .sessionName("claude-dev")
                .windowName("ai-assistant")
                .detached(true)
                .logFile("/var/log/claude-session.log")
                .build())
            .build();
        
        claudeCli.execute("개발 서버 시작해줘", options);
        
        // 나중에 세션 확인
        String output = tmuxManager.capturePane("claude-dev");
        System.out.println("Session output: " + output);
    }
}
```

### 병렬 실행

```java
public void parallelExecution() {
    List<String> prompts = Arrays.asList(
        "Python 코드 작성해줘",
        "Java 코드 작성해줘",
        "JavaScript 코드 작성해줘"
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

### REST API 통합 예제

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

## 📁 프로젝트 구조

```
spring-claude-cli-adapter/
├── src/main/java/com/claudecli/adapter/
│   ├── core/                    # 핵심 인터페이스 및 실행 엔진
│   ├── service/                 # 서비스 구현체
│   ├── model/                   # 데이터 모델
│   ├── security/                # 보안 정책
│   ├── config/                  # Spring Boot 자동 설정
│   └── util/                    # 유틸리티
├── examples/                    # 예제 애플리케이션
└── pom.xml
```

## 🤝 기여하기

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 Apache License 2.0 라이선스 하에 배포됩니다.

## 🔗 관련 링크

- [Claude CLI 공식 문서](https://docs.anthropic.com/claude-cli)
- [Spring Boot 문서](https://spring.io/projects/spring-boot)
- [예제 프로젝트](./examples/demo-app)

## ❓ FAQ

**Q: Claude CLI는 어떻게 설치하나요?**
A: [Claude CLI 설치 가이드](https://docs.anthropic.com/claude-cli/installation)를 참고하세요.

**Q: API 키는 어디서 얻나요?**
A: [Anthropic Console](https://console.anthropic.com)에서 API 키를 발급받을 수 있습니다.

**Q: 여러 사용자가 동시에 사용할 수 있나요?**
A: 네, 세션 관리 기능을 통해 각 사용자별로 독립된 대화 컨텍스트를 유지할 수 있습니다.

**Q: 프로덕션 환경에서 사용해도 되나요?**
A: 보안 정책을 적절히 설정하고, API 사용량을 모니터링하면서 사용하시기 바랍니다.