package com.lwz.ads.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 功能: 线程池阻塞时, 分析阻塞原因
 *
 * @author liweizhou 2020/11/23
 */
public class SmartRejectedExecutionHandler implements RejectedExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(SmartRejectedExecutionHandler.class);

    private int analyzeCount = 1;

    private final ConcurrentMap<Executor, Executor> executorConcurrentMap = new ConcurrentHashMap<>();

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        if (executor.isShutdown()) {
            log.info("ThreadPool is shutdown! Discard task!");
            return;
        }

        log.error("ThreadPool is full! Discard task! executor:{} task:{}", executor, r);

        Executor analyzer = executorConcurrentMap.computeIfAbsent(executor, key -> new ThreadPoolExecutor(
                1, 1,
                1, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(1),
                new CustomizableThreadFactory("analyze-"),
                new ThreadPoolExecutor.DiscardPolicy()));

        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        analyzer.execute(() -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                analyze(executor);
            } finally {
                if (contextMap != null) {
                    MDC.clear();
                }
            }
        });

    }

    private void analyze(ThreadPoolExecutor executor) {

        try {
            List<Thread> threads = getThreads(executor);

            threads.stream().map(Thread::getState).collect(Collectors.groupingBy(Function.identity()))
                    .forEach((state, stateList) -> {
                        log.info("analyze state:{} size:{}", state, stateList.size());
                    });

            List<ThreadWrapper> wrappers = new ArrayList<>();
            for (Thread thread : threads) {
                try {
                    StackTraceElement[] stackTrace = thread.getStackTrace();
                    ThreadWrapper threadWrapper = new ThreadWrapper();
                    threadWrapper.stackTrace = stackTrace;
                    int index = wrappers.indexOf(threadWrapper);
                    if (index > -1) {
                        wrappers.get(index).threadList.add(thread);
                    } else {
                        threadWrapper.threadList.add(thread);
                        wrappers.add(threadWrapper);
                    }
                } catch (SecurityException e) {
                    log.warn("ThreadPoolAnalyze permission limited! return. err:{}", e.getMessage());
                    return;
                }
            }

            wrappers.sort((o1, o2) -> Integer.compare(o2.threadList.size(), o1.threadList.size()));

            wrappers.stream().limit(analyzeCount).forEach(threadWrapper -> {
                String trace = Stream.of(threadWrapper.stackTrace).map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n\t"));
                log.warn("ThreadPoolAnalyze same stack count:{} trace:\n\t{}", threadWrapper.threadList.size(), trace);
            });

        } catch (Throwable e) {
            log.error("ThreadPoolAnalyze fail. err:{}", e.getMessage(), e);
        }
    }

    private List<Thread> getThreads(ThreadPoolExecutor executor) {
        Field mainLockField = ReflectionUtils.findField(ThreadPoolExecutor.class, "mainLock");
        mainLockField.setAccessible(true);
        Lock mainLock = (Lock)ReflectionUtils.getField(mainLockField, executor);

        List workers = new ArrayList();
        mainLock.lock();
        try {
            Field workersField = ReflectionUtils.findField(ThreadPoolExecutor.class, "workers");
            workersField.setAccessible(true);
            Set workerSet = (Set) ReflectionUtils.getField(workersField, executor);
            workers.addAll(workerSet);
        } finally {
            mainLock.unlock();
        }

        if (workers.isEmpty()) {
            return Collections.emptyList();
        }

        Object theOne = workers.iterator().next();
        Field threadField = ReflectionUtils.findField(theOne.getClass(), "thread");
        threadField.setAccessible(true);

        List<Thread> threads = new ArrayList<>(workers.size());
        for (Object worker : workers) {
            Thread thread = (Thread) ReflectionUtils.getField(threadField, worker);
            if (thread != null) {
                threads.add(thread);
            }
        }
        return threads;
    }

    public int getAnalyzeCount() {
        return analyzeCount;
    }

    public void setAnalyzeCount(int analyzeCount) {
        this.analyzeCount = analyzeCount;
    }

    @Override
    public String toString() {
        return "SmartRejectedExecutionHandler{" +
                "analyzeCount=" + analyzeCount +
                '}';
    }

    public class ThreadWrapper {
        List<Thread> threadList = new ArrayList<>();
        StackTraceElement[] stackTrace;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ThreadWrapper)) return false;
            ThreadWrapper that = (ThreadWrapper) o;
            return Arrays.equals(stackTrace, that.stackTrace);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(stackTrace);
        }
    }

}
