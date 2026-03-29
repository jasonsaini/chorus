# Chorus API contract

This document is the source of truth for the HTTP and WebSocket API between the Chorus backend and clients (for example the React app). Update this file **before** changing payloads, destinations, or persistence behavior, then align the README if the user-facing summary should stay in sync.

---

## Base URL

- Local default: `http://localhost:8080`
- WebSocket/SockJS endpoint path: `/ws` (browser URL is typically `http://localhost:8080/ws` with SockJS)

---

## Persistence (server behavior)

| Data | Persisted? | Notes |
|------|------------|--------|
| Rooms (`roomId`, `inviteCode`, `createdBy`, `createdAt`) | Yes | Stored in relational DB (default: H2 file under `CHORUS_DATA_DIR`). |
| Chat history in REST/WebSocket payload shape | Yes, **only** `USER` and `AI` | Ordered by insert time. Used for `GET /api/rooms/{roomId}` and for Claude context. |
| Participants (usernames) | Yes | Updated when clients successfully **join** or disconnect (**leave**). |
| `JOIN`, `LEAVE`, `AI_TYPING`, `ERROR` over WebSocket | No | Delivered live only; not replayed from `GET` history. |

---

## REST

### `POST /api/rooms`

Creates a new room.

**Request body**

```json
{
  "createdBy": "username"
}
```

**Response `201 Created`**

```json
{
  "roomId": "abc123def456",
  "inviteCode": "abc123def456",
  "createdBy": "Jason",
  "createdAt": "2026-03-28T14:30:00Z"
}
```

`inviteCode` is currently the same value as `roomId` (the client may treat it as the shareable code).

**Validation**

- `createdBy` is required and must not be blank (`400` with default Spring validation error body if invalid).

---

### `GET /api/rooms/{roomId}`

Returns room metadata needed to **replay chat after refresh**: current participants (from DB) and persisted messages (`USER` and `AI` only).

**Response `200 OK`**

```json
{
  "roomId": "abc123def456",
  "participants": ["Jason", "Priya"],
  "messages": [
    {
      "type": "USER",
      "sender": "Jason",
      "content": "Hello",
      "timestamp": "2026-03-28T14:32:05Z"
    }
  ]
}
```

**Response `404 Not Found`**

Room id does not exist.

**CORS**

`GET` and `POST` are allowed from origins listed in `CHORUS_ALLOWED_ORIGINS` (comma-separated; default `http://localhost:3000`).

---

## WebSocket (STOMP over SockJS)

### Connection

- Clients connect with **SockJS** to the STOMP endpoint registered at **`/ws`**.
- Use a STOMP client (for example **STOMP.js**) and subscribe to the room topic below before or immediately after joining.

### Application prefix (client → server)

The application destination prefix is **`/app`**.

### Destinations (client → server)

| Destination | Body (JSON) | Description |
|-------------|---------------|-------------|
| `/app/chat.join` | `{ "roomId": "…", "username": "…" }` | Join a room. Room must exist (create it via REST first). Registers the WebSocket session and adds the username to persisted participants. Broadcasts `JOIN` to the room topic. |
| `/app/chat.send` | `{ "roomId": "…", "sender": "…", "content": "…" }` | Send a human message. **Requires** a prior successful `chat.join` for this connection with the **same** `roomId` and `username` as `sender`. Persists and broadcasts `USER`. May trigger Claude (see **AI trigger** below). |

**Server error handling (same topic)**

If join or send fails a server-side check (unknown room, send without join, etc.), the server may publish an `ERROR` message to `/topic/room/{roomId}` (see below). Clients should still subscribe to that topic to surface errors.

### Subscriptions (server → client)

Subscribe to:

```text
/topic/room/{roomId}
```

All broadcast events for that room are published on this topic.

### Message envelope (server → client)

Every frame uses the same JSON shape:

```json
{
  "type": "USER",
  "sender": "Jason",
  "content": "What if we add auth middleware?",
  "timestamp": "2026-03-28T14:32:05Z"
}
```

- `timestamp` is ISO-8601 instant (UTC) with `Z` suffix in JSON serialization.
- `content` may be an empty string for some types (for example `JOIN` / `LEAVE`).

### Message types

| `type` | Meaning |
|--------|---------|
| `JOIN` | A user joined (WebSocket + persisted participant). |
| `LEAVE` | A user disconnected; participant removed from DB. |
| `USER` | Human message; **persisted**. |
| `AI_TYPING` | Claude is generating; **not** persisted. |
| `AI` | Claude reply; **persisted**. |
| `ERROR` | Server-side failure (for example LLM error); **not** persisted in history. |

### AI trigger (`chorus.ai.trigger`)

| Value | Behavior |
|-------|-----------|
| `always` (default) | After each persisted `USER` message, the server requests a Claude reply. |
| `mention` | Claude runs only if the user message contains the substring `@claude` (case-insensitive). |

Configured in `application.yml` under `chorus.ai.trigger`.

---

## Environment variables

| Variable | Purpose |
|----------|---------|
| `ANTHROPIC_API_KEY` | Anthropic API key for Claude (Spring AI). |
| `CHORUS_ALLOWED_ORIGINS` | Comma-separated browser origins for CORS and WebSocket handshake. |
| `CHORUS_DATA_DIR` | Optional directory path for the H2 database files (default `./data` relative to the process working directory). |

---

## Versioning

Breaking changes to this contract should be called out in commit messages and PR descriptions. Prefer additive changes (new fields optional) when possible.
