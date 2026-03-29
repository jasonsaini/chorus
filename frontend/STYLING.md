# Chorus frontend — styling guidelines

This document defines the visual language for the Chorus React app so new UI stays consistent with the backend-powered chat experience.

## Principles

- **Dark-first**: The UI assumes a dark canvas so long chat sessions stay comfortable. Avoid pure black; use layered surfaces for depth.
- **Clarity over decoration**: Prefer readable typography, obvious hierarchy, and stable layout over ornamental effects.
- **Real-time affordances**: Connection state and “Claude is thinking” should be visible but calm — never shout over the conversation.
- **Match the contract**: UI copy and behavior align with `docs/contract.md` (message types, REST + WebSocket flows).

## Tokens (`src/styles/tokens.css`)

Design tokens are CSS custom properties. **Do not hard-code hex values in components** unless adding a new token here first.

| Token | Role |
|-------|------|
| `--color-bg`, `--color-bg-elevated`, `--color-bg-muted` | Page, cards, nested panels |
| `--color-border`, `--color-border-strong` | Dividers, inputs, bubbles |
| `--color-text`, `--color-text-muted`, `--color-text-subtle` | Body, secondary labels, meta |
| `--color-accent`, `--color-accent-soft` | Primary actions, links, focus |
| `--color-user`, `--color-user-bg` | Human messages |
| `--color-ai`, `--color-ai-bg` | Claude / AI messages and typing |
| `--color-danger`, `--color-danger-bg` | `ERROR` messages and destructive emphasis |

Typography:

- **Sans**: `DM Sans` for UI and chat (`--font-sans`).
- **Mono**: `JetBrains Mono` for room IDs, code snippets (`--font-mono`).

Radii: `--radius-sm` for controls, `--radius-md` / `--radius-lg` for cards and bubbles, `--radius-full` for pills and dots.

## Layout

- **Home**: Centered card, max width ~440px, generous padding. One primary action (Create), one secondary path (Join).
- **Room**: Two-column grid on wide screens (sidebar + main). Sidebar collapses to a top band on small viewports (`max-width: 840px`).
- **Chat feed**: Scrollable column with messages anchored to the bottom (auto-scroll on new content). Composer is fixed to the bottom of the main column.

## Message presentation (maps to `MessageType`)

| Type | Treatment |
|------|------------|
| `USER` | Right-aligned bubble, user accent (`--color-user-*`). Show sender + time. |
| `AI` | Left-aligned bubble, AI accent (`--color-ai-*`). Label as Claude. |
| `JOIN` / `LEAVE` | Centered compact system line — no bubble. |
| `AI_TYPING` | Subtle “thinking” row with animated dots; do not add a fake `AI` message. |
| `ERROR` | Left-aligned alert bubble using danger tokens; still show timestamp. |

Persisted history from `GET /api/rooms/{id}` is only `USER` and `AI`; live-only types come from WebSocket — the UI must handle both sources without duplicating rows.

## Components & CSS

- **Scoped styles**: Prefer CSS Modules (`*.module.css`) colocated with the page or component. Avoid global class names except in `global.css` and `tokens.css`.
- **Global rules**: `global.css` resets, focus rings, and body background only.
- **Buttons**: Primary = gradient accent; secondary = ghost with border. Disabled state lowers opacity; do not rely on color alone for critical meaning.
- **Forms**: Full-width inputs on mobile; visible labels; `aria` where needed for live regions (typing indicator).

## Accessibility

- Visible focus styles via `--focus-ring` on interactive elements.
- Sufficient contrast for text on `--color-bg-elevated` and bubbles.
- `aria-live` on typing / transient status where appropriate.

## Environment & API

- Default dev setup uses the Vite proxy (`/api`, `/ws`) to `localhost:8080`. For split deployments, set `VITE_API_ORIGIN` at build time to the backend origin (see `src/config.ts`).

## When you change UI

1. Update tokens first if you introduce a new recurring color or radius.
2. Keep this file in sync when adding new patterns (e.g. modals, toasts).
3. If the change affects API assumptions, update `docs/contract.md` before shipping.
