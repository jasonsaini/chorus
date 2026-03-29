# Chorus

A real-time collaborative chat where humans and AI think together.

## What is this?

Chorus lets multiple people share a single AI context window — think of it as a group chat where Claude is a participant, not a separate tab each person has open on their own machine. Everyone sees the same conversation, the same AI responses, and builds on the same context in real time.

Built as a monorepo with a Spring Boot backend and a JavaScript frontend.

---
TODOS: 
### Pending Items / Future Features

- [ ] Different colors for users on the frontend
- [ ] Pull in Github repos for context
- [ ] Some sort of coding collaborative interface
- [ ] Notify Claude when users are kicked from the chat
---

## Monorepo Structure

```
chorus/
├── backend/       # Spring Boot — WebSocket server, Claude integration, room management
├── frontend/      # React — chat UI, room join/create flow, real-time message rendering
├── docs/
│   └── contract.md   # WebSocket + REST API contract (read this first)
└── README.md
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring WebSocket (STOMP), Spring AI, Spring Data JPA |
| Storage | H2 (file-backed by default; see `CHORUS_DATA_DIR`) |
| LLM | Anthropic Claude (via Spring AI) |
| Frontend | React, SockJS, STOMP.js |
| Real-time | WebSocket with SockJS fallback |

---

## Getting Started

### Prerequisites

- Java 21+
- Node 18+
- An [Anthropic API key](https://console.anthropic.com)

---

### Backend

```bash
cd backend
cp src/main/resources/application.example.yml src/main/resources/application.yml
# Add your ANTHROPIC_API_KEY to application.yml
./mvnw spring-boot:run
```

Server runs on `http://localhost:8080`.

OpenAPI/Swagger UI: **`http://localhost:8080/swagger-ui.html`** (JSON: `/v3/api-docs`). WebSocket/STOMP is not listed there; see `docs/contract.md`.

Rooms, participants, and chat lines (`USER` and `AI` only) are stored under `data/` (or `CHORUS_DATA_DIR` if set) as H2 files so restarts keep history. Ephemeral WebSocket events (`JOIN`, `LEAVE`, `AI_TYPING`, `ERROR`) are not part of that replayable history.

**Optional — verify Claude end-to-end (uses your API key; small usage cost):**

```bash
cd backend
export ANTHROPIC_API_KEY=sk-ant-api03-...
./mvnw test -Dtest=AnthropicLiveIT
```

`AnthropicLiveIT` is **skipped** when `ANTHROPIC_API_KEY` is unset. Default `./mvnw test` keeps a mocked `ChatModel` and does not call Anthropic.

---

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`.

---

## API Contract

### WebSocket

Connect via SockJS at `ws://localhost:8080/ws`.

**Destinations (client → server):**

| Destination | Payload | Description |
|---|---|---|
| `/app/chat.join` | `{ roomId, username }` | Join a room |
| `/app/chat.send` | `{ roomId, sender, content }` | Send a message |

**Subscription (server → client):**

Subscribe to `/topic/room/{roomId}` to receive all messages for a room.

**Message types:**

| Type | Description |
|---|---|
| `JOIN` | A user joined the room |
| `LEAVE` | A user left the room |
| `USER` | A human sent a message |
| `AI_TYPING` | Claude is generating a response |
| `AI` | Claude's response |
| `ERROR` | Something went wrong server-side |

**Message shape:**
```json
{
  "type": "USER",
  "sender": "Jason",
  "content": "What if we add auth middleware?",
  "timestamp": "2026-03-28T14:32:05Z"
}
```

---

### REST

#### Create a room
```
POST /api/rooms
Body: { "createdBy": "username" }

Response 201:
{
  "roomId": "abc123",
  "inviteCode": "abc123",
  "createdBy": "Jason",
  "createdAt": "2026-03-28T14:30:00Z"
}
```

#### Get room + history
```
GET /api/rooms/{roomId}

Response 200:
{
  "roomId": "abc123",
  "participants": ["Jason", "Priya"],
  "messages": [ ... ]
}
```

Call this on page load to replay history after a refresh. Persisted messages are **`USER` and `AI` only**; live-only events (`JOIN`, `LEAVE`, `AI_TYPING`, `ERROR`) come from WebSockets. See `docs/contract.md` for full behavior.

---

## @claude Trigger

By default, Claude responds to every message. To switch to mention-only mode (Claude responds only when `@claude` appears in the message), set the following in `application.yml`:

```yaml
chorus:
  ai:
    trigger: mention   # options: always | mention
```

---

## Environment Variables

| Variable | Description |
|---|---|
| `ANTHROPIC_API_KEY` | Your Anthropic API key |
| `CHORUS_ALLOWED_ORIGINS` | Comma-separated frontend origins (default: `http://localhost:3000`) |
| `CHORUS_DATA_DIR` | Directory for H2 database files (default: `./data` relative to where you run the server) |
| `CHORUS_OPENAPI_SERVER_URL` | Optional base URL for OpenAPI “Servers” dropdown (e.g. `http://your-lan-ip:8080`) |

---

## Contributing

This is a two-person project split across backend and frontend. Before making changes that affect the API contract, update `docs/contract.md` first and align with your collaborator.

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Open a PR against `main`

---

## License

MIT
