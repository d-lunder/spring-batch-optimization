package com.example.demo;

import com.example.demo.model.Transactions;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Component
public class MultithreadedPerformanceMonitor implements JobExecutionListener,
        ItemProcessListener<Transactions, Transactions> {

    private static final Logger logger = LoggerFactory.getLogger(MultithreadedPerformanceMonitor.class);
    private static final int SAMPLING_INTERVAL_MS = 1000;

    private final LongAdder processedItems = new LongAdder();
    private final AtomicLong startTime = new AtomicLong(0);

    private final AtomicLong peakHeapUsageMB = new AtomicLong(0);
    private final LongAdder heapUsageSamplesSum = new LongAdder();
    private final LongAdder sampleCount = new LongAdder();

    private final ConcurrentHashMap<String, LongAdder> threadItemCounts = new ConcurrentHashMap<>();

    private ScheduledExecutorService memoryMonitor;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        processedItems.reset();
        startTime.set(System.currentTimeMillis());
        peakHeapUsageMB.set(0);
        heapUsageSamplesSum.reset();
        sampleCount.reset();
        threadItemCounts.clear();

        logger.info("Starting job: {}", jobExecution.getJobInstance().getJobName());

        memoryMonitor = Executors.newSingleThreadScheduledExecutor();
        memoryMonitor.scheduleAtFixedRate(
                this::sampleMemory,
                0,
                SAMPLING_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {
        if (memoryMonitor != null) {
            memoryMonitor.shutdown();
            try {
                if (!memoryMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                    memoryMonitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                memoryMonitor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        sampleMemory();

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime.get();

        double recordsPerSecond = durationMs > 0
                ? (processedItems.sum() * 1000.0) / durationMs
                : 0;

        double avgHeapUsageMB = sampleCount.sum() > 0
                ? (double) heapUsageSamplesSum.sum() / sampleCount.sum()
                : 0;

        logger.info("Job completed: {}", jobExecution.getJobInstance().getJobName());
        logger.info("Duration: {} ms", durationMs);
        logger.info("Total items processed: {}", processedItems.sum());
        logger.info("Average processing rate: {} records/second", recordsPerSecond);
        logger.info("Peak heap usage: {} MB", peakHeapUsageMB.get());
        logger.info("Average heap usage: {} MB", avgHeapUsageMB);
    }

    @Override
    public void beforeProcess(@NonNull Transactions item) {}

    @Override
    public void afterProcess(@NonNull Transactions item, Transactions result) {
        processedItems.increment();

        String threadName = Thread.currentThread().getName();
        threadItemCounts.computeIfAbsent(threadName, k -> new LongAdder()).increment();
    }

    @Override
    public void onProcessError(@NonNull Transactions item, @NonNull Exception e) {}

    private void sampleMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        long usedHeapMB = heapUsage.getUsed() / (1024 * 1024);

        peakHeapUsageMB.updateAndGet(peak -> Math.max(peak, usedHeapMB));

        heapUsageSamplesSum.add(usedHeapMB);
        sampleCount.increment();
    }
}
