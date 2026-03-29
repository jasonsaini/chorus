import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { createRoom } from "../api/rooms";
import { setStoredUsername } from "../lib/username";
import styles from "./HomePage.module.css";

export function HomePage() {
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const nameOk = displayName.trim().length > 0;

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!nameOk) return;
    setError(null);
    setBusy(true);
    try {
      const created = await createRoom(displayName.trim());
      setStoredUsername(displayName.trim());
      navigate(`/room/${created.roomId}`, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create room.");
    } finally {
      setBusy(false);
    }
  }

  function onJoin(e: FormEvent) {
    e.preventDefault();
    if (!nameOk) return;
    const code = inviteCode.trim();
    if (!code) {
      setError("Enter a room code.");
      return;
    }
    setError(null);
    setStoredUsername(displayName.trim());
    navigate(`/room/${encodeURIComponent(code)}`, { replace: true });
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.card}>
        <h1 className={styles.title}>Chorus</h1>
        <p className={styles.sub}>
          Shared AI context for your team — one room, one thread, everyone in sync.
        </p>

        <span className={styles.sectionLabel}>Your name</span>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="displayName">
            Display name
          </label>
          <input
            id="displayName"
            className={styles.input}
            autoComplete="nickname"
            placeholder="How others see you"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
        </div>

        <form onSubmit={onCreate}>
          <div className={styles.row}>
            <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`} disabled={busy || !nameOk}>
              Create room
            </button>
          </div>
        </form>

        <div className={styles.divider}>or join</div>

        <form onSubmit={onJoin}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="invite">
              Room code
            </label>
            <input
              id="invite"
              className={styles.input}
              placeholder="Paste invite code"
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value)}
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
            />
          </div>
          <div className={styles.row}>
            <button type="submit" className={`${styles.btn} ${styles.btnGhost}`} disabled={busy || !nameOk}>
              Join room
            </button>
          </div>
        </form>

        {error ? <div className={styles.error}>{error}</div> : null}

        <p className={styles.footer}>
          Runs against the Chorus API — start the backend on port 8080, then use this app on port 3000.
        </p>
      </div>
    </div>
  );
}
