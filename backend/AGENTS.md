# Backend Agent Guidelines

Spring Boot 3.4 / Java 21 backend for Chorus. Read this before touching any code.

## Architecture

```
config/       - Spring config beans and @ConfigurationProperties
domain/       - Immutable domain objects (records, no JPA annotations)
service/      - Business logic, transactional, no HTTP/WebSocket concerns
ws/           - STOMP WebSocket controllers and session management
web/          - REST controllers and DTOs
persistence/  - JPA entities and repositories only
```

Never mix layers. Domain objects do not know about JPA. Services do not know about HTTP or STOMP. Controllers do not talk to repositories directly.

## Java Style

- Use **records** for immutable data: domain objects, DTOs, payload classes
- Use **constructor injection** — no `@Autowired` on fields
- No `var` except in method bodies where the type is obvious from context
- Imports must be fully qualified at the top — never inline package names in code
- All service methods that write to the DB must be `@Transactional`
- Read-only service methods must be `@Transactional(readOnly = true)`

## Domain Layer

- `ChatMessage` and `Room` are immutable records — do not add setters or JPA annotations
- `Room` uses defensive copies (`List.copyOf`, `new LinkedHashSet<>`) — keep it that way
- Only `MessageType.USER` and `MessageType.AI` are persisted — others are ephemeral broadcasts

## Persistence Layer

- JPA entities live in `persistence/` only — one entity per file
- Entities must have a `protected no-arg constructor` for Hibernate
- Use `FetchType.LAZY` on all `@ManyToOne` relationships
- Never return JPA entities from services — always map to domain objects

## Services

- `RoomService` owns all DB reads/writes — other services call it, not the repositories
- `AiReplyService.replyAsync()` is the only entry point for AI calls — call it from controllers, not `ChatAiService.generate()` directly
- `conversationForModel()` applies a token budget sliding window — do not bypass it

## WebSocket

- STOMP endpoint: `/ws` (SockJS enabled)
- App prefix: `/app`, topic prefix: `/topic`
- Session state lives in `SessionRegistry` (ConcurrentHashMap) — always check binding before allowing sends
- Heartbeat is configured at 25s — do not remove it or the connection drops on Railway

## Configuration

- All tuneable values belong in `ChorusProperties` — never hardcode URLs, origins, or limits
- Env vars follow the pattern `CHORUS_*` — document any new ones in `application.yml`
- `ANTHROPIC_API_KEY` is required in production — app will start without it but AI calls will fail

## Tests

- Integration tests use `@ActiveProfiles("test")` which switches to in-memory H2
- Mock `ChatModel` with `@MockBean` in tests that don't need real AI
- Live tests that call the real Anthropic API must be annotated with `@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")` so CI doesn't call the API

## What Not To Do

- Do not add `@Autowired` field injection
- Do not add JPA annotations to domain objects
- Do not call `chatAiService.generate()` directly from a controller
- Do not add `System.out.println` — use SLF4J (`LoggerFactory.getLogger`)
- Do not catch and swallow exceptions silently
- Do not add speculative features, extra config options, or abstractions not required by the task
