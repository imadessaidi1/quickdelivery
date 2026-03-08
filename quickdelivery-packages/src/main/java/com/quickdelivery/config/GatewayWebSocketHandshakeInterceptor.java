package com.quickdelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class GatewayWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String GATEWAY_TOKEN_HEADER = "X-Gateway-Token";

    @Value("${quickdelivery.gateway.internal-token}")
    private String gatewayInternalToken;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getHeader(GATEWAY_TOKEN_HEADER);
            if (!gatewayInternalToken.equals(token)) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
        // No-op
    }
}
