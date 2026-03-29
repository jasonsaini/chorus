import { apiBase } from "../config";
import type { CreateRoomResponse, RoomDetailResponse } from "../types";

export async function createRoom(createdBy: string): Promise<CreateRoomResponse> {
  const res = await fetch(`${apiBase()}/rooms`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ createdBy }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to create room (${res.status})`);
  }
  return res.json() as Promise<CreateRoomResponse>;
}

export async function getRoom(roomId: string): Promise<RoomDetailResponse> {
  const res = await fetch(`${apiBase()}/rooms/${encodeURIComponent(roomId)}`);
  if (res.status === 404) {
    throw new Error("Room not found.");
  }
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to load room (${res.status})`);
  }
  return res.json() as Promise<RoomDetailResponse>;
}
