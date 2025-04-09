package com.quickdelivery.abstarct.aspect;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExceptionLoggingAspect {
    @Autowired
    private Logger logger;

    @AfterThrowing(pointcut = "execution(* com.quickdelivery..*(..))", throwing = "exception")
    public void logException(Exception exception) {
        String exceptionMessage = exception.getMessage();
        String exceptionStackTrace = getStackTraceAsString(exception);
        logger.error("Exception occurred: " + exceptionMessage);
        logger.error("Stack Trace: " + exceptionStackTrace);
    }

    private String getStackTraceAsString(Exception exception) {
        StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement element : exception.getStackTrace()) {
            stackTrace.append(element.toString()).append("\n");
        }
        return stackTrace.toString();
    }
}

