package com.example.demo.batch;

import com.example.demo.model.Transactions;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;

@Configuration
public class SharedBatchBeans {

    private static final String FILE_PATH = "D:/transactions_output.txt";

    @Bean
    public JdbcPagingItemReader<Transactions> pagingItemReader(DataSource dataSource, PagingQueryProvider queryProvider) {
        return new JdbcPagingItemReaderBuilder<Transactions>()
                .name("transactionPagingReader")
                .dataSource(dataSource)
                .fetchSize(100000)
                .queryProvider(queryProvider)
                .rowMapper(new BeanPropertyRowMapper<>(Transactions.class))
                .build();
    }

    @Bean
    public PagingQueryProvider queryProvider(DataSource dataSource) throws Exception {
        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource);
        factory.setSelectClause("SELECT id, transaction_date, amount, created_at");
        factory.setFromClause("FROM transactions");
        factory.setSortKey("id");
        return factory.getObject();
    }

    @Bean
    public TransactionItemProcessor itemProcessor() {
        return new TransactionItemProcessor();
    }

    @Bean
    public FlatFileItemWriter<Transactions> flatFileItemWriter() {
        return new FlatFileItemWriterBuilder<Transactions>()
                .name("transactionFlatFileWriter")
                .resource(new FileSystemResource(FILE_PATH))
                .delimited()
                .delimiter(", ")
                .names("id", "transactionDate", "amount", "createdAt")
                .build();
    }
}
