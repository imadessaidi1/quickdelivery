package com.quickdelivery.abstarct.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;

//@Aspect
@Component
public class HttpRequestLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestLoggingAspect.class);

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {}

    @Before("restController()")
    public void logBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        logger.info("Request URL: " + request.getRequestURL().toString());
        logger.info("Request Method: " + request.getMethod());
        logger.info("Request Headers: " + request.getHeaderNames());
        logger.info("Request Parameters: " + request.getParameterMap());
        logger.info("Request Length: " + request.getContentLength());
        logger.info("Request Body: " + getRequestBody(request));
    }

    @AfterReturning(pointcut = "restController()", returning = "response")
    public void logAfter(JoinPoint joinPoint, Object response) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse httpResponse = attributes.getResponse();
        logger.info("Response Status: " + httpResponse.getStatus());
        logger.info("Response Status: " + httpResponse.getStatus());
        logger.info("Response Body: " + response);
    }

    private String getRequestBody(HttpServletRequest request) {
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Error reading request body", e);
        }
        return requestBody.toString();
    }
}
