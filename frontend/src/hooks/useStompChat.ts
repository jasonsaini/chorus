import { Client, type IMessage } from "@stomp/stompjs";
import { useCallback, useEffect, useRef, useState } from "react";
import SockJS from "sockjs-client";
import { sockJsUrl } from "../config";
import type { ChatMessage, MessageType } from "../types";

export type ChatHandler = (msg: ChatMessage) => void;

function parseMessage(raw: IMessage): ChatMessage | null {
  try {
    const data = JSON.parse(raw.body) as {
      type: MessageType;
      sender: string;
      content?: string;
      timestamp: string;
    };
    return {
      type: data.type,
      sender: data.sender,
      content: data.content ?? "",
      timestamp: data.timestamp,
    };
  } catch {
    return null;
  }
}

export function useStompChat(
  roomId: string | undefined,
  username: string | undefined,
  enabled: boolean,
  onMessage: ChatHandler,
) {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const onMessageRef = useRef<ChatHandler>(onMessage);
  onMessageRef.current = onMessage;

  useEffect(() => {
    if (!enabled || !roomId || !username) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl()) as unknown as WebSocket,
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
    });

    client.onConnect = () => {
      setConnected(true);
      client.subscribe(`/topic/room/${roomId}`, (message) => {
        const parsed = parseMessage(message);
        if (parsed) {
          onMessageRef.current(parsed);
        }
      });
      client.publish({
        destination: "/app/chat.join",
        body: JSON.stringify({ roomId, username }),
      });
    };

    client.onStompError = () => {
      setConnected(false);
    };

    client.onWebSocketClose = () => {
      setConnected(false);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [enabled, roomId, username]);

  const sendMessage = useCallback(
    (content: string) => {
      const c = clientRef.current;
      if (!c?.connected || !roomId || !username) {
        return;
      }
      c.publish({
        destination: "/app/chat.send",
        body: JSON.stringify({ roomId, sender: username, content }),
      });
    },
    [roomId, username],
  );

  return { connected, sendMessage };
}
