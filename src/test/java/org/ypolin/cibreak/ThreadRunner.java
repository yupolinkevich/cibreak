package org.ypolin.cibreak;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadRunner {
    public record Statistics(int succeededThreads, int failedThreads, ConcurrentMap<Class,Integer> exceptions) {}

    public static Statistics runTaskByThreads(int n, Runnable task) {
        List<Runnable> tasks = Collections.nCopies(n, task);
        return runTasks(tasks);
    }

    public static Statistics runTasks(List<Runnable> tasks){
        ConcurrentMap<Class, Integer> errorsStats = new ConcurrentHashMap<>();
        AtomicInteger succeededThreads = new AtomicInteger(0);
        AtomicInteger failedThreads = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(tasks.size());
        tasks.stream()
                .map((task) -> new Thread(() -> {
                    try {
                        task.run();
                        succeededThreads.incrementAndGet();
                    } catch (Exception ex) {
                        failedThreads.incrementAndGet();
                        errorsStats.compute(ex.getClass(), (k, v) -> v == null ? 1 : v + 1);
                    } finally {
                        countDownLatch.countDown();
                    }
                }))
                .forEach(Thread::start);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new Statistics(succeededThreads.get(), failedThreads.get(),errorsStats);
    }
}
