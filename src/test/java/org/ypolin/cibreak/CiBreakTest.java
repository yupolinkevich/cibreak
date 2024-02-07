package org.ypolin.cibreak;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.ypolin.cibreak.demo.ExtServiceUnavailableException;
import org.ypolin.cibreak.exception.CiBreakException;
import org.ypolin.cibreak.handler.CiBreakState;
import org.ypolin.cibreak.handler.CiBreaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
class CiBreakTest {
    @Autowired
    private CiBreaker ciBreaker;

    @Test
    void verifySwitchFromClosedToOpenState() {
        ciBreaker.setClosed();
        List<Runnable> successfulRequests = Collections.nCopies(4, prepareRequest(true));
        List<Runnable> failedRequests = Collections.nCopies(6, prepareRequest(false));
        List<Runnable> requests = new ArrayList<>(successfulRequests);
        requests.addAll(failedRequests);
        ThreadRunner.Statistics callsStat = ThreadRunner.runTasks(requests);
        try {
            //make sure failure rate monitor is started
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(CiBreakState.OPEN, ciBreaker.getState());
        assertEquals(4, callsStat.succeededThreads());
        assertEquals(6, callsStat.failedThreads());
    }

    @Test
    void verifySwitchFromOpenToHalfOpenState() {
        ciBreaker.setOpen();
        try {
            //Wait for switching to half-open state. Sleep time must be >= halfOpenWaitTime
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(CiBreakState.HALF_OPEN, ciBreaker.getState());
    }

    @Test
    void verifySwitchFromHalfOpenToClosedState() {
        ciBreaker.setHalfOpen();
        ThreadRunner.Statistics statistics = ThreadRunner.runTaskByThreads(5, prepareRequest(true));
        assertEquals(CiBreakState.CLOSED, ciBreaker.getState());
        assertEquals(3, statistics.succeededThreads());
        assertEquals(2, statistics.failedThreads());
        assertEquals(2,statistics.exceptions().get(CiBreakException.class));
    }

    @Test
    void verifySwitchFromHalfOpenToOpenState(){
        ciBreaker.setHalfOpen();

        List<Runnable> successfulRequests = Collections.nCopies(2, prepareRequest(true));
        List<Runnable> failedRequests = Collections.nCopies(3, prepareRequest(false));
        List<Runnable> requests = new ArrayList<>(successfulRequests);
        requests.addAll(failedRequests);

        ThreadRunner.runTasks(requests);

        assertEquals(CiBreakState.OPEN, ciBreaker.getState());
    }


    @Test
    void verifyInOpenState(){
        ciBreaker.setOpen();
        ThreadRunner.Statistics callsStat = ThreadRunner.runTasks(List.of(prepareRequest(false), prepareRequest(true)));
        assertEquals(CiBreakState.OPEN, ciBreaker.getState());
        assertEquals(callsStat.failedThreads(), 2);
        assertEquals(callsStat.succeededThreads(), 0);
        assertEquals(2, callsStat.exceptions().get(CiBreakException.class));
    }

    @Test
    void verifyThatOnlySpecifiedExceptionIsConsidered(){
        ciBreaker.setClosed();
        List<Runnable> successfulRequests = Collections.nCopies(4, prepareRequest(true));
        List<Runnable> logicallyFailedRequests = Collections.nCopies(6, () -> ciBreaker.call(() -> {
            throw new IllegalArgumentException("any logical exception");
        }));
        List<Runnable> requests = new ArrayList<>(successfulRequests);
        requests.addAll(logicallyFailedRequests);

        ThreadRunner.Statistics callsStat = ThreadRunner.runTasks(requests);

        assertEquals(CiBreakState.CLOSED, ciBreaker.getState());
        assertEquals(4, callsStat.succeededThreads());
        assertEquals(6, callsStat.failedThreads());
        assertEquals(1, callsStat.exceptions().size());
        assertEquals(6, callsStat.exceptions().get(IllegalArgumentException.class));
    }
    private Runnable prepareRequest(boolean successful) {
        return () -> ciBreaker.call(() -> {
            //emulate call to external service
            try {
                //a bit of response time
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!successful) {
                throw new ExtServiceUnavailableException("503. Service unavailable");
            }
            return new ResponseEntity<>(HttpStatusCode.valueOf(200));
        });
    }
}