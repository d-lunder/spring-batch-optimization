package com.example.demo.batch.multithreaded;

import com.example.demo.MultithreadedPerformanceMonitor;
import com.example.demo.batch.TransactionItemProcessor;
import com.example.demo.model.Transactions;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MultithreadedJobConfig {

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
    MultithreadedPerformanceMonitor multithreadedPerformanceMonitor;

    @Bean
    @Qualifier("multithreaded")
    public Job multithreadedJob(JobRepository jobRepository, Step multithreadedStep) {
        return new JobBuilder("multithreadedJob", jobRepository)
                .start(multithreadedStep)
                .listener(multithreadedPerformanceMonitor)
                .build();
    }

    @Bean
    public Step multithreadedStep(TaskExecutor taskExecutor) {
        return new StepBuilder("multithreadedStep", jobRepository)
                .<Transactions, Transactions>chunk(1000, transactionManager)
                .reader(pagingItemReader)
                .processor(itemProcessor)
                .writer(flatFileItemWriter)
                .listener(multithreadedPerformanceMonitor)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(40);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("multithreadedJob-");
        executor.initialize();
        return executor;
    }
}

