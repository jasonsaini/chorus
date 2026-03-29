# Chorus

A real-time collaborative chat where humans and AI think together.

## What is this?

Chorus lets multiple people share a single AI context window — think of it as a group chat where Claude is a participant, not a separate tab each person has open on their own machine. Everyone sees the same conversation, the same AI responses, and builds on the same context in real time.

Built as a monorepo with a Spring Boot backend and a JavaScript frontend.

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
| Backend | Java 21, Spring Boot 3, Spring WebSocket (STOMP), Spring AI |
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

Call this on page load to replay history after a refresh.

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
