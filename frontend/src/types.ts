export type MessageType = "JOIN" | "LEAVE" | "USER" | "AI_TYPING" | "AI" | "ERROR";

export interface ChatMessage {
  type: MessageType;
  sender: string;
  content: string;
  timestamp: string;
}

export interface CreateRoomResponse {
  roomId: string;
  inviteCode: string;
  createdBy: string;
  createdAt: string;
}

export interface RoomDetailResponse {
  roomId: string;
  participants: string[];
  messages: ChatMessage[];
}
