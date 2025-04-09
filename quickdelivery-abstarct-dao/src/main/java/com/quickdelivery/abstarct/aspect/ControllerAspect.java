package com.quickdelivery.abstarct.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ControllerAspect {
    @Autowired
    private Logger logger;
    ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("execution(* com.quickdelivery.controllers.*.*(..))")
    private void controllerExecution() {}

    @Before("controllerExecution()")
    public void logBeforeControllerExecution(JoinPoint joinPoint) {
        startTime.set(System.currentTimeMillis());
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        logger.info("Start controller method: " + methodName + " with args: " + argsToString(args));
    }

    @After("controllerExecution()")
    public void logAfterControllerExecution(JoinPoint joinPoint) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime.get();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        logger.info("End controller method: " + methodName + " with args: " + argsToString(args));
        logger.info("Execution time of " + methodName + ": " + duration + " milliseconds");
    }

    private String argsToString(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object arg : args) {
            sb.append(arg).append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("]");
        return sb.toString();
    }
}
