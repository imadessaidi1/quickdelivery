package com.quickdelivery.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.MessageDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.PositionDTO;
import com.quickdelivery.services.interfaces.IPackagesService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private Logger logger;

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    @Autowired
    private IPackagesService packagesService;
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("Received message from client " + session.getId() + ": " + message.getPayload());
        ObjectMapper objectMapper = new ObjectMapper();
        MessageDTO messageDTO = null;
        try {
            messageDTO = objectMapper.readValue(message.getPayload(), MessageDTO.class);
            Map<String, PositionDTO> map = packagesService.handleWebsocketMessage(messageDTO);
            MessageDTO response = new MessageDTO();
            response.setType(messageDTO.getType());
            response.setFrom(messageDTO.getTo());
            response.setTo(messageDTO.getFrom());
            response.setMessage(objectMapper.writeValueAsString(map));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageToAll(String message) {
        for (WebSocketSession session : sessions) {
            if(session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                logger.info("Couldn't send notification, session closed");
            }
        }
    }
}
