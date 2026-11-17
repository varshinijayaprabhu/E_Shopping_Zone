package com.eshopping.wallet.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Enterprise Spring AOP Logging Aspect for Wallet Service.
 * Intercepts method execution across Controller and Service layers.
 * Decouples cross-cutting concerns (diagnostics, execution timing, error monitoring)
 * from core business logic.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Pointcut matching all Spring REST Controllers.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerPointcut() {
        // Pointcut marker
    }

    /**
     * Pointcut matching all business services in the service layer.
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void servicePointcut() {
        // Pointcut marker
    }

    /**
     * Around advice logging method entry, parameter types, execution duration, and exit.
     */
    @Around("restControllerPointcut() || servicePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        if (log.isDebugEnabled()) {
            log.debug("--> [AOP-ENTER] {}.{}() with args: {}", className, methodName, sanitizeArgs(args));
        } else {
            log.info("--> [AOP-ENTER] {}.{}()", className, methodName);
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            if (log.isDebugEnabled()) {
                log.debug("<-- [AOP-EXIT]  {}.{}() executed in {} ms", className, methodName, executionTime);
            } else {
                log.info("<-- [AOP-EXIT]  {}.{}() executed in {} ms", className, methodName, executionTime);
            }
            return result;
        } catch (Throwable ex) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("!! [AOP-EXCEPTION] in {}.{}() after {} ms with message: {}",
                    className, methodName, executionTime, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Sanitizes arguments to prevent sensitive credentials from leaking into log files.
     */
    private String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.toString(Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return "null";
                    String str = arg.toString();
                    if (str.toLowerCase().contains("password") || str.toLowerCase().contains("secret")) {
                        return "[PROTECTED_CREDENTIALS]";
                    }
                    return str;
                })
                .toArray());
    }
}
