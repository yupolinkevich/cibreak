package org.ypolin.cibreak.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CiBreakTaskScheduler {
    private static Logger log = LoggerFactory.getLogger(CiBreakTaskScheduler.class);
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    @Value("${cibreak.halfOpenTimeout}")
    private long halfOpenTimeout;
    @Value("${cibreak.failureMonitoringFrequency}")
    private long failureMonitoringFrequency;
    private ScheduledFuture<?> monitorFailuresTaskState;
    private ScheduledFuture<?> switchToHalOpenTaskState;

    public void scheduleFailuresMonitoring(Runnable failuresMonitoringTask) {
        if (monitorFailuresTaskState != null && !monitorFailuresTaskState.isDone()) {
            return;
        }
        monitorFailuresTaskState = executorService.scheduleWithFixedDelay(() -> {
            log.info("Monitoring system failures...");
            try {
                failuresMonitoringTask.run();
            } catch (Exception ex) {
                log.error("Error occurred while monitoring system failures.", ex);
            }
        }, failureMonitoringFrequency, failureMonitoringFrequency, TimeUnit.MILLISECONDS);
    }

    public void scheduleSwitchToHalfOpenState(Runnable halfOpenSwitcher){
        if (switchToHalOpenTaskState != null && !switchToHalOpenTaskState.isDone()) {
            return;
        }
        switchToHalOpenTaskState = executorService.schedule(() -> {
            log.info("Switching CiBreaker to HALF-OPEN state...");
            try {
                halfOpenSwitcher.run();
            } catch (Exception ex) {
                log.error("Error occurred while switching CiBreaker to HALF-OPEN state.", ex);
            }
        },  halfOpenTimeout, TimeUnit.MILLISECONDS);
    }

    public void cancelFailuresMonitoring(){
        if (monitorFailuresTaskState != null && !monitorFailuresTaskState.isDone()) {
            boolean cancelled = monitorFailuresTaskState.cancel(false);
            log.info(String.format("Monitoring system failures is %s", cancelled? "cancelled": "not cancelled"));
        }
    }

    public void cancelSwitchToHalfOpen(){
        if (switchToHalOpenTaskState != null && !switchToHalOpenTaskState.isDone()) {
            boolean cancelled = switchToHalOpenTaskState.cancel(false);
            log.info(String.format("Switching to HALF-OPEN state is %s", cancelled? "cancelled": "not cancelled"));
        }
    }
}
