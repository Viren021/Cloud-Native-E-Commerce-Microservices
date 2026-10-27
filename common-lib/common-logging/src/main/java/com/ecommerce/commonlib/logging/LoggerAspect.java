package com.ecommerce.commonlib.logging;

import com.ecommerce.commonlib.constants.MdcKey;
import com.ecommerce.commonlib.util.SensitiveDataSanitizer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * AOP aspect for {@link LogPerformance} and {@link Loggable} annotations.
 *
 * <p>Registered as a bean by {@code LoggingAutoConfiguration} — NOT via {@code @Component} —
 * so it is discovered regardless of the service's component-scan base package.
 *
 * <h3>Performance log format (INFO via "perfLogger")</h3>
 * <pre>
 * [action=Service.method] [duration=123ms] [result=SUCCESS] [title=...] [corrId=...] [input=...] [output=...]
 * </pre>
 *
 * <h3>Loggable log format (DEBUG via LoggerAspect logger)</h3>
 * <pre>
 * → Service.method(arg1, arg2)
 * ← Service.method → returnValue   (or WARN on error)
 * </pre>
 */
@Aspect
@RequiredArgsConstructor
public class LoggerAspect {

    private static final Logger perfLog = LoggerFactory.getLogger("perfLogger");
    private static final Logger log     = LoggerFactory.getLogger(LoggerAspect.class);

    private final PerformanceLogProperties props;

    // ----------------------------------------------------------------
    // @LogPerformance — duration + optional input/output
    // ----------------------------------------------------------------

    @Around("@annotation(logPerformance)")
    public Object aroundLogPerformance(ProceedingJoinPoint pjp,
                                       LogPerformance logPerformance) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String action = pjp.getTarget().getClass().getSimpleName() + "." + sig.getName();
        String title  = logPerformance.title().isBlank() ? action : logPerformance.title();

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;
            if (duration >= props.getThresholdMs()) {
                String inputs  = logPerformance.logInput()  ? summarize(pjp.getArgs()) : "-";
                String output  = logPerformance.logOutput() ? summarize(result)         : "-";
                logPerf(action, title, duration, LogField.SUCCESS, inputs, output);
            }
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            logPerf(action, title, duration, LogField.ERROR, "-", t.getClass().getSimpleName());
            throw t;
        }
    }

    // ----------------------------------------------------------------
    // @Loggable — entry / exit trace
    // ----------------------------------------------------------------

    @Around("@annotation(loggable)")
    public Object aroundLoggable(ProceedingJoinPoint pjp,
                                 Loggable loggable) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String action = pjp.getTarget().getClass().getSimpleName() + "." + sig.getName();

        if (loggable.logInput()) {
            log.debug("{}({})", action, summarize(pjp.getArgs()));
        } else {
            log.debug("{}()", action);
        }

        try {
            Object result = pjp.proceed();
            if (loggable.logOutput()) {
                log.debug("{} → {}", action, summarize(result));
            } else {
                log.debug("{} completed", action);
            }
            return result;
        } catch (Throwable t) {
            log.warn("{} ERROR: {}", action, t.getClass().getSimpleName());
            throw t;
        }
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private void logPerf(String action, String title, long durationMs,
                         String result, String inputs, String output) {
        String corrId = MDC.get(MdcKey.CORRELATION_ID);
        perfLog.info("[action={}] [duration={}ms] [result={}] [title={}] [corrId={}] [input={}] [output={}]",
                action, durationMs, result, title, corrId, inputs, output);
    }

    private static String summarize(Object value) {
        return SensitiveDataSanitizer.summarize(value);
    }
}
