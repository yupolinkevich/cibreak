package org.ypolin.cibreak.handler;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class CiBreakAspect {
    @Autowired
    private CiBreaker ciBreaker;

    @Around("execution(public * *(..)) && @annotation(org.ypolin.cibreak.handler.CiBreak)")
    public Object handle(ProceedingJoinPoint point) {
        return ciBreaker.call(() -> {
            try {
                return point.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

}
