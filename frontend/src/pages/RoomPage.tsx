import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getRoom } from "../api/rooms";
import { useStompChat } from "../hooks/useStompChat";
import { getStoredUsername, setStoredUsername } from "../lib/username";
import type { ChatMessage } from "../types";
import styles from "./RoomPage.module.css";

function formatTime(iso: string): string {
  try {
    const d = new Date(iso);
    return new Intl.DateTimeFormat(undefined, { hour: "numeric", minute: "2-digit" }).format(d);
  } catch {
    return "";
  }
}

export function RoomPage() {
  const { roomId: roomIdParam } = useParams();
  const roomId = roomIdParam ? decodeURIComponent(roomIdParam) : undefined;

  const [username, setUsername] = useState(() => getStoredUsername() ?? "");
  const [nameDraft, setNameDraft] = useState("");
  const [roomLoaded, setRoomLoaded] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [participants, setParticipants] = useState<string[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [aiTyping, setAiTyping] = useState(false);
  const [composer, setComposer] = useState("");
  const feedRef = useRef<HTMLDivElement>(null);

  const canChat = roomLoaded && username.trim().length > 0;

  useEffect(() => {
    if (!roomId) {
      setLoadError("Missing room.");
      return;
    }
    let cancelled = false;
    setLoadError(null);
    setRoomLoaded(false);
    getRoom(roomId)
      .then((r) => {
        if (cancelled) return;
        setParticipants(r.participants);
        setMessages(r.messages);
        setRoomLoaded(true);
      })
      .catch((err) => {
        if (cancelled) return;
        setLoadError(err instanceof Error ? err.message : "Could not load room.");
      });
    return () => {
      cancelled = true;
    };
  }, [roomId]);

  useEffect(() => {
    const el = feedRef.current;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
  }, [messages, aiTyping]);

  const onWsMessage = useCallback((msg: ChatMessage) => {
    switch (msg.type) {
      case "JOIN":
        setParticipants((p) => (p.includes(msg.sender) ? p : [...p, msg.sender]));
        setMessages((m) => [...m, msg]);
        break;
      case "LEAVE":
        setParticipants((p) => p.filter((x) => x !== msg.sender));
        setMessages((m) => [...m, msg]);
        break;
      case "USER":
      case "AI":
        setMessages((m) => [...m, msg]);
        if (msg.type === "AI") {
          setAiTyping(false);
        }
        break;
      case "AI_TYPING":
        setAiTyping(true);
        break;
      case "ERROR":
        setMessages((m) => [...m, msg]);
        setAiTyping(false);
        break;
      default:
        break;
    }
  }, []);

  const { connected, sendMessage } = useStompChat(roomId, username.trim(), canChat, onWsMessage);

  function confirmName(e: FormEvent) {
    e.preventDefault();
    const next = nameDraft.trim();
    if (!next) return;
    setStoredUsername(next);
    setUsername(next);
  }

  const shareUrl = useMemo(() => {
    if (typeof window === "undefined" || !roomId) return "";
    return `${window.location.origin}/room/${encodeURIComponent(roomId)}`;
  }, [roomId]);

  async function copyInvite() {
    if (!shareUrl || !navigator.clipboard) return;
    try {
      await navigator.clipboard.writeText(shareUrl);
    } catch {
      /* ignore */
    }
  }

  function onSend(e: FormEvent) {
    e.preventDefault();
    const text = composer.trim();
    if (!text || !connected) return;
    sendMessage(text);
    setComposer("");
  }

  function onComposerKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key !== "Enter" || e.shiftKey) return;
    e.preventDefault();
    const text = composer.trim();
    if (!text || !connected) return;
    sendMessage(text);
    setComposer("");
  }

  if (!roomId) {
    return (
      <div className={styles.center}>
        <div className={styles.centerCard}>
          <h2>Invalid link</h2>
          <Link to="/">Back home</Link>
        </div>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className={styles.center}>
        <div className={styles.centerCard}>
          <h2>Room unavailable</h2>
          <p>{loadError}</p>
          <Link to="/">Back home</Link>
        </div>
      </div>
    );
  }

  if (!roomLoaded) {
    return (
      <div className={styles.center}>
        <div className={styles.centerCard}>
          <p>Loading room…</p>
        </div>
      </div>
    );
  }

  const needsName = !username.trim();

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <Link to="/">← Chorus</Link>
        </div>
        <div>
          <div className={styles.meta}>Room</div>
          <div className={styles.meta} title={roomId}>
            <code>{roomId}</code>
          </div>
          <div className={styles.copyRow}>
            <button type="button" className={styles.copyBtn} onClick={() => void copyInvite()}>
              Copy invite link
            </button>
          </div>
        </div>
        <div>
          <div className={styles.participantTitle}>People</div>
          <ul className={styles.participantList}>
            {participants.map((p) => (
              <li key={p} className={styles.participant}>
                <span className={styles.dot} aria-hidden />
                {p}
              </li>
            ))}
            {participants.length === 0 ? (
              <li className={styles.meta} style={{ listStyle: "none" }}>
                No one connected yet.
              </li>
            ) : null}
          </ul>
        </div>
      </aside>

      <section className={styles.main}>
        <header className={styles.toolbar}>
          <h1 className={styles.roomTitle}>Room chat</h1>
          <div className={styles.status}>
            <span
              className={`${styles.statusDot} ${connected ? styles.statusDotLive : ""}`}
              aria-hidden
            />
            {connected ? "Live" : "Connecting…"}
          </div>
        </header>

        {needsName ? (
          <div className={styles.banner} style={{ background: "var(--color-bg-muted)", color: "var(--color-text)" }}>
            <form onSubmit={confirmName} style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap", alignItems: "center" }}>
              <label htmlFor="name" style={{ fontSize: "var(--text-sm)" }}>
                Choose your display name for this session:
              </label>
              <input
                id="name"
                className={styles.textarea}
                style={{ minHeight: 36, maxWidth: 220 }}
                value={nameDraft}
                onChange={(e) => setNameDraft(e.target.value)}
                placeholder={getStoredUsername() ?? "Your name"}
              />
              <button type="submit" className={styles.send} disabled={!nameDraft.trim()}>
                Continue
              </button>
            </form>
          </div>
        ) : null}

        <div ref={feedRef} className={styles.feed}>
          {messages.length === 0 && !aiTyping ? (
            <p className={styles.empty}>
              Say hello — everyone in this room shares the same Claude context.
            </p>
          ) : null}

          {messages.map((m, i) => {
            if (m.type === "JOIN" || m.type === "LEAVE") {
              return (
                <div key={`${m.timestamp}-${i}`} className={styles.system}>
                  {m.type === "JOIN" ? `${m.sender} joined` : `${m.sender} left`}
                </div>
              );
            }
            if (m.type === "USER") {
              return (
                <div key={`${m.timestamp}-${i}`} className={`${styles.msgRow} ${styles.msgRowUser}`}>
                  <div className={`${styles.bubble} ${styles.bubbleUser}`}>
                    <div className={styles.metaLine}>
                      <span className={`${styles.sender} ${styles.senderUser}`}>{m.sender}</span>
                      <span className={styles.time}>{formatTime(m.timestamp)}</span>
                    </div>
                    <div className={styles.body}>{m.content}</div>
                  </div>
                </div>
              );
            }
            if (m.type === "AI") {
              return (
                <div key={`${m.timestamp}-${i}`} className={`${styles.msgRow} ${styles.msgRowAi}`}>
                  <div className={`${styles.bubble} ${styles.bubbleAi}`}>
                    <div className={styles.metaLine}>
                      <span className={`${styles.sender} ${styles.senderAi}`}>Claude</span>
                      <span className={styles.time}>{formatTime(m.timestamp)}</span>
                    </div>
                    <div className={styles.body}>{m.content}</div>
                  </div>
                </div>
              );
            }
            if (m.type === "ERROR") {
              return (
                <div key={`${m.timestamp}-${i}`} className={`${styles.msgRow} ${styles.msgRowError}`}>
                  <div className={`${styles.bubble} ${styles.bubbleError}`}>
                    <div className={styles.metaLine}>
                      <span className={styles.sender}>Error</span>
                      <span className={styles.time}>{formatTime(m.timestamp)}</span>
                    </div>
                    <div className={styles.body}>{m.content}</div>
                  </div>
                </div>
              );
            }
            return null;
          })}
        </div>

        {aiTyping ? (
          <div className={styles.typingBar} aria-live="polite">
            <span>Claude is thinking</span>
            <span className={styles.typingDots} aria-hidden>
              <span />
              <span />
              <span />
            </span>
          </div>
        ) : null}

        <footer className={styles.composerWrap}>
          <form className={styles.composer} onSubmit={onSend}>
            <textarea
              className={styles.textarea}
              rows={2}
              placeholder={connected ? "Message the room…" : "Connecting…"}
              value={composer}
              onChange={(e) => setComposer(e.target.value)}
              disabled={!connected || needsName}
              onKeyDown={onComposerKeyDown}
            />
            <button type="submit" className={styles.send} disabled={!connected || needsName || !composer.trim()}>
              Send
            </button>
          </form>
          <p className={styles.hint}>
            Everyone sees the same thread. Use <code>@claude</code> if the server is in mention-only mode.
          </p>
        </footer>
      </section>
    </div>
  );
}
