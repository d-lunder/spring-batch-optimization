package com.example.demo.batch.chunk;

import com.example.demo.ChunkPerformanceMonitor;
import com.example.demo.batch.TransactionItemProcessor;
import com.example.demo.model.Transactions;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ChunkJobConfig {

    @Autowired
    JobRepository jobRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcPagingItemReader<Transactions> pagingItemReader;

    @Autowired
    TransactionItemProcessor itemProcessor;

    @Autowired
    FlatFileItemWriter<Transactions> flatFileItemWriter;

    @Autowired
    ChunkPerformanceMonitor chunkPerformanceMonitor;

    @Bean
    @Qualifier("chunk")
    public Job chunkJob(JobRepository jobRepository, Step chunkStep) {
        return new JobBuilder("chunkJob", jobRepository)
                .start(chunkStep)
                .listener(chunkPerformanceMonitor)
                .build();
    }

    @Bean
    public Step chunkStep() {
        return new StepBuilder("chunkStep", jobRepository)
                .<Transactions, Transactions>chunk(1000, transactionManager)
                .reader(pagingItemReader)
                .processor(itemProcessor)
                .writer(flatFileItemWriter)
                .listener((ItemProcessListener<? super Transactions, ? super Transactions>) chunkPerformanceMonitor)
                .listener((ChunkListener) chunkPerformanceMonitor)
                .build();
    }
}
