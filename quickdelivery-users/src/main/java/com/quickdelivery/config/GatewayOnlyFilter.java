package com.quickdelivery.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayOnlyFilter implements Filter {

    private static final String GATEWAY_TOKEN_HEADER = "X-Gateway-Token";

    @Value("${quickdelivery.gateway.internal-token}")
    private String gatewayInternalToken;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        if (uri != null && uri.startsWith("/users/v1/")) {
            String receivedToken = httpRequest.getHeader(GATEWAY_TOKEN_HEADER);
            if (!gatewayInternalToken.equals(receivedToken)) {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResponse.getWriter().write("{\"error\":\"FORBIDDEN_DIRECT_ACCESS\",\"message\":\"Use API Gateway\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
