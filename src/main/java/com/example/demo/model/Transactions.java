package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transactions {

    private Integer id;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private LocalDate createdAt;
}
