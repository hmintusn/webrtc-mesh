package com.SFU;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler extends TextWebSocketHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionToPeerId = new ConcurrentHashMap<>();
    private static final Map<String, String> peerIdToSession = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        WebSocketMessage msg = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);

        switch (msg.getType()) {
            case "join":
                handleJoinMessage(session, msg);
                break;
            case "leave":
                handleLeaveMessage(session, msg);
                break;
            case "offer":
            case "answer":
            case "candidate":
                forwardMessage(msg);
                break;
        }
    }

    private void handleJoinMessage(WebSocketSession session, WebSocketMessage msg) throws IOException {
        // Store the mapping between session and peerId
        sessionToPeerId.put(session.getId(), msg.getPeerId());
        peerIdToSession.put(msg.getPeerId(), session.getId());

        // Notify all other peers about the new peer
        WebSocketMessage joinMsg = new WebSocketMessage();
        joinMsg.setType("user-joined");
        joinMsg.setPeerId(msg.getPeerId());

        String joinMsgString = objectMapper.writeValueAsString(joinMsg);
        for (WebSocketSession ws : sessions.values()) {
            if (!ws.getId().equals(session.getId())) {
                ws.sendMessage(new TextMessage(joinMsgString));
            }
        }
    }

    private void handleLeaveMessage(WebSocketSession session, WebSocketMessage msg) throws IOException {
        String peerId = sessionToPeerId.get(session.getId());
        if (peerId != null) {
            // Notify all other peers about the leaving peer
            WebSocketMessage leaveMsg = new WebSocketMessage();
            leaveMsg.setType("user-left");
            leaveMsg.setPeerId(peerId);

            String leaveMsgString = objectMapper.writeValueAsString(leaveMsg);
            for (WebSocketSession ws : sessions.values()) {
                if (!ws.getId().equals(session.getId())) {
                    ws.sendMessage(new TextMessage(leaveMsgString));
                }
            }
        }
    }

    private void forwardMessage(WebSocketMessage msg) throws IOException {
        // Forward message to specific peer
        String targetSessionId = peerIdToSession.get(msg.getTo());
        if (targetSessionId != null) {
            WebSocketSession targetSession = sessions.get(targetSessionId);
            if (targetSession != null && targetSession.isOpen()) {
                targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        String peerId = sessionToPeerId.get(session.getId());
        if (peerId != null) {
            // Clean up mappings
            sessionToPeerId.remove(session.getId());
            peerIdToSession.remove(peerId);

            // Notify others about peer leaving
            WebSocketMessage leaveMsg = new WebSocketMessage();
            leaveMsg.setType("user-left");
            leaveMsg.setPeerId(peerId);

            String leaveMsgString = objectMapper.writeValueAsString(leaveMsg);
            for (WebSocketSession ws : sessions.values()) {
                if (!ws.getId().equals(session.getId()) && ws.isOpen()) {
                    ws.sendMessage(new TextMessage(leaveMsgString));
                }
            }
        }

        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("Transport error: " + exception.getMessage());
    }
}