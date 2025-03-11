package com.example.demo;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicLong;

@Component
@JobScope
public class ChunkPerformanceMonitor implements ChunkListener, JobExecutionListener, ItemProcessListener<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ChunkPerformanceMonitor.class);

    private long startTime;
    private final AtomicLong processedItems = new AtomicLong(0);
    private final AtomicLong lastLogTime = new AtomicLong(0);
    private final AtomicLong lastProcessedCount = new AtomicLong(0);
    private final AtomicLong totalHeap = new AtomicLong(0);
    private final AtomicLong samplesCount = new AtomicLong(0);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = System.currentTimeMillis();
        lastLogTime.set(startTime);
        processedItems.set(0);
        lastProcessedCount.set(0);

        logger.info("Job started: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        long totalProcessed = processedItems.get();

        double overallRate = (totalProcessed * 1000.0) / duration;
        double averageHeapUsage = 0;
        if (samplesCount.get() > 0) {
            averageHeapUsage = (double) totalHeap.get() / samplesCount.get();
        }

        logger.info("Job completed: {}", jobExecution.getJobInstance().getJobName());
        logger.info("Duration: {} ms", duration);
        logger.info("Total items processed: {}", totalProcessed);
        logger.info("Average processing rate: {} records/second", overallRate);
        logger.info("Average heap usage: {} MB", averageHeapUsage);
    }

    @Override
    public void beforeChunk(@NonNull ChunkContext context) {
    }

    @Override
    public void afterChunk(@NonNull ChunkContext context) {
        long currentTime = System.currentTimeMillis();
        long currentProcessedCount = processedItems.get();

        lastLogTime.set(currentTime);
        lastProcessedCount.set(currentProcessedCount);

        sampleMemory();
    }

    @Override
    public void afterChunkError(@NonNull ChunkContext context) {
        sampleMemory();
    }

    @Override
    public void beforeProcess(@NonNull Object item) {
    }

    @Override
    public void afterProcess(@NonNull Object item, Object result) {
        processedItems.incrementAndGet();
    }

    @Override
    public void onProcessError(@NonNull Object item, Exception e) {
        logger.error("Error processing item: {}", e.getMessage());
    }

    private void sampleMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        long usedHeapMB = heapUsage.getUsed() / (1024 * 1024);

        totalHeap.addAndGet(usedHeapMB);
        samplesCount.incrementAndGet();
    }
}
