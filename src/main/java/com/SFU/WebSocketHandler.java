package com.SFU;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionToPeerId = new ConcurrentHashMap<>();
    private static final Map<String, String> peerIdToSession = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Connection established with session ID: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        log.info("Received message from session ID {}", session.getId());
        
        WebSocketMessage msg = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
        log.debug("Parsed message type: {}", msg.getType());
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
            default:
                log.warn("Unknown message type received: {}", msg.getType());
        }
    }

    private void handleJoinMessage(WebSocketSession session, WebSocketMessage msg) throws IOException {
        // Store the mapping between session and peerId
        sessionToPeerId.put(session.getId(), msg.getPeerId());
        peerIdToSession.put(msg.getPeerId(), session.getId());
        log.info("User joined with peer ID: {}", msg.getPeerId());

        // Notify all other peers about the new peer
        WebSocketMessage joinMsg = new WebSocketMessage();
        joinMsg.setType("user-joined");
        joinMsg.setPeerId(msg.getPeerId());

        String joinMsgString = objectMapper.writeValueAsString(joinMsg);
        for (WebSocketSession ws : sessions.values()) {
            if (!ws.getId().equals(session.getId())) {
                asyncSendMessage(ws, new TextMessage(joinMsgString));
            }
        }
        log.info("Broadcasted 'user-joined' message for peer ID: {}", msg.getPeerId());
    }

    private void handleLeaveMessage(WebSocketSession session, WebSocketMessage msg) throws IOException {
        String peerId = sessionToPeerId.get(session.getId());
        if (peerId != null) {
            log.info("User leaving with peer ID: {}", peerId);

            WebSocketMessage leaveMsg = new WebSocketMessage();
            leaveMsg.setType("user-left");
            leaveMsg.setPeerId(peerId);

            String leaveMsgString = objectMapper.writeValueAsString(leaveMsg);
            for (WebSocketSession ws : sessions.values()) {
                if (!ws.getId().equals(session.getId())) {
                    asyncSendMessage(ws, new TextMessage(leaveMsgString));
                }
            }
            log.info("Broadcasted 'user-left' message for peer ID: {}", peerId);
        }
    }

    private void forwardMessage(WebSocketMessage msg) throws IOException {
        // Forward message to specific peer
        String targetSessionId = peerIdToSession.get(msg.getTo());
        if (targetSessionId != null) {
            WebSocketSession targetSession = sessions.get(targetSessionId);
            if (targetSession != null && targetSession.isOpen()) {
                asyncSendMessage(targetSession, new TextMessage(objectMapper.writeValueAsString(msg)));
                log.info("Forwarded message from {} to {}", msg.getFrom(), msg.getTo());
            } else {
                log.warn("Target session {} is not open or does not exist", targetSessionId);
            }
        } else {
            log.warn("No target session found for peer ID: {}", msg.getTo());
        }
    }

    private void asyncSendMessage(WebSocketSession session, TextMessage message) {
        executorService.submit(() -> {
            try {
                synchronizedSendMessage(session, message);
            } catch (IOException e) {
                log.error("Failed to send message asynchronously", e);
            }
        });
    }

    private void synchronizedSendMessage(WebSocketSession session, TextMessage message) throws IOException {
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        String peerId = sessionToPeerId.get(session.getId());
        if (peerId != null) {
            sessionToPeerId.remove(session.getId());
            peerIdToSession.remove(peerId);
            log.info("Connection closed for peer ID: {}", peerId);

            // Notify others about peer leaving
            WebSocketMessage leaveMsg = new WebSocketMessage();
            leaveMsg.setType("user-left");
            leaveMsg.setPeerId(peerId);

            String leaveMsgString = objectMapper.writeValueAsString(leaveMsg);
            for (WebSocketSession ws : sessions.values()) {
                if (!ws.getId().equals(session.getId()) && ws.isOpen()) {
                    asyncSendMessage(ws, new TextMessage(leaveMsgString));
                }
            }
            log.info("Broadcasted 'user-left' message for peer ID: {}", peerId);
        }

        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error in session {}: {}", session.getId(), exception.getMessage(), exception);
    }

    // Clean up executor service on shutdown
    public void shutdown() {
        executorService.shutdown();
    }
}
