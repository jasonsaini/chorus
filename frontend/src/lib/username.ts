const KEY = "chorus.username";

export function getStoredUsername(): string | null {
  try {
    return localStorage.getItem(KEY);
  } catch {
    return null;
  }
}

export function setStoredUsername(name: string): void {
  try {
    localStorage.setItem(KEY, name.trim());
  } catch {
    /* ignore */
  }
}
