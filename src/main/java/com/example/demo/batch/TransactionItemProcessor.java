package com.example.demo.batch;

import com.example.demo.model.Transactions;
import lombok.NonNull;
import org.springframework.batch.item.ItemProcessor;

public class TransactionItemProcessor implements ItemProcessor<Transactions, Transactions> {
    @Override
    public Transactions process(@NonNull Transactions item) throws Exception {
        Thread.sleep(1);
        return item;
    }
}
