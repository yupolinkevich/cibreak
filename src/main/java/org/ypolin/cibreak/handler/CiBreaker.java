package org.ypolin.cibreak.handler;

import com.fasterxml.jackson.core.JsonToken;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ypolin.cibreak.exception.CiBreakException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Service
public class CiBreaker {
    @Value("${cibreak.failureThreshold}")
    private double failureThreshold;
    @Value("${cibreak.successThreshold}")
    private int successThreshold;
    @Value("${cibreak.exceptionType}")
    private String exceptionType;
    private volatile CiBreakState state = CiBreakState.CLOSED;
    private volatile AtomicInteger totalCalls = new AtomicInteger(0);
    private volatile AtomicInteger failedCalls = new AtomicInteger(0);
    private volatile AtomicInteger successCalls = new AtomicInteger(0);
    private static Logger log = LoggerFactory.getLogger(CiBreaker.class);
    @Autowired
    private CiBreakTaskScheduler taskScheduler;


    public <T> T call(Supplier<T> supplier) {
        return switch (state) {
            case OPEN -> {
                log.info(String.format("State: %s. No calls to external service allowed.", state));
                throw new CiBreakException("No calls to external service allowed");
            }
            case HALF_OPEN -> {
                log.info(String.format("State: %s. Probing external service", state));
                if (totalCalls.incrementAndGet() > successThreshold) {
                    throw new CiBreakException("Calls to external service are not permitted");
                }
                try {
                    T res = supplier.get();
                    if (successCalls.incrementAndGet() >= successThreshold) {
                        log.info(String.format("External service is available. Moving to CLOSED."));
                        setClosed();
                    }
                    yield res;
                } catch (Exception ex) {
                    if (ex.getClass().getName().equals(exceptionType)) {
                        int prevFailedCalls = failedCalls.getAndIncrement();
                        //first failed call sets CiBreaker to OPEN
                        if (prevFailedCalls == 0) {
                            setOpen();
                        }
                    }
                    throw ex;
                }
            }
            case CLOSED -> {
                log.info(String.format("State: %s. Redirected to external service", state));
                int totalCallsPrev = totalCalls.getAndIncrement();
                //first call starts the monitoring of failure rate
                if (totalCallsPrev == 0) {
                    taskScheduler.scheduleFailuresMonitoring(this::checkFailureRate);
                }
                try {
                    yield supplier.get();
                } catch (Exception ex) {
                    if (ex.getClass().getName().equals(exceptionType)) {
                        failedCalls.incrementAndGet();
                    }
                    throw ex;
                }
            }
        };
    }

    public void setOpen() {
        taskScheduler.cancelFailuresMonitoring();
        taskScheduler.cancelSwitchToHalfOpen();
        state = CiBreakState.OPEN;
        taskScheduler.scheduleSwitchToHalfOpenState(this::setHalfOpen);
    }

    public void setHalfOpen() {
        taskScheduler.cancelFailuresMonitoring();
        state = CiBreakState.HALF_OPEN;
        totalCalls.set(0);
        failedCalls.set(0);
        successCalls.set(0);
    }

    public void setClosed() {
        state = CiBreakState.CLOSED;
        failedCalls.set(0);
        totalCalls.set(0);
        taskScheduler.scheduleFailuresMonitoring(this::checkFailureRate);
    }

    private void checkFailureRate() {
        if (totalCalls.get() == 0) {
            return;
        }
        double failureRate = failedCalls.get() / (double) totalCalls.get();
        if (failureRate >= failureThreshold) {
            log.info(String.format("Failure rate = %.2f. Moving to OPEN.", failureRate));
            setOpen();
        } else {
            totalCalls.set(0);
            failedCalls.set(0);
        }
    }
    public CiBreakState getState() {
        return state;
    }
}
