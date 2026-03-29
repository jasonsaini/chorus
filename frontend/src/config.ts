/**
 * Empty string = same origin (Vite dev proxy to backend, or app served behind same host).
 * Set `VITE_API_ORIGIN` (e.g. http://localhost:8080) when the API is on another origin.
 */
export function apiBase(): string {
  const o = import.meta.env.VITE_API_ORIGIN?.replace(/\/$/, "") ?? "";
  return o ? `${o}/api` : "/api";
}

export function sockJsUrl(): string {
  const o = import.meta.env.VITE_API_ORIGIN?.replace(/\/$/, "") ?? "";
  return o ? `${o}/ws` : "/ws";
}
