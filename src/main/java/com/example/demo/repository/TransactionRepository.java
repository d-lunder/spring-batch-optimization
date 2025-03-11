package com.example.demo.repository;

import com.example.demo.model.Transactions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void setSchema() {
        jdbcTemplate.execute("SET search_path TO public");
    }

    private static class TransactionRowMapper implements RowMapper<Transactions> {
        @Override
        public Transactions mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Transactions(
                    rs.getInt("id"),
                    rs.getDate("transaction_date").toLocalDate(),
                    rs.getBigDecimal("amount"),
                    rs.getDate("created_at") != null ? rs.getDate("created_at").toLocalDate() : null
            );
        }
    }

    // Fetch all transactions
    public List<Transactions> findAll() {
        String sql = "SELECT * FROM transactions";
        return jdbcTemplate.query(sql, new TransactionRowMapper());
    }

    // Fetch by ID
    public Transactions findById(int id) {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new TransactionRowMapper(), id);
    }

    // Insert a transaction
    public int save(Transactions transaction) {
        String sql = "INSERT INTO transactions (id, transaction_date, amount, created_at) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getAmount(),
                transaction.getCreatedAt());
    }

    // Update a transaction
    public int update(Transactions transaction) {
        String sql = "UPDATE transactions SET transaction_date = ?, amount = ? WHERE id = ?";
        return jdbcTemplate.update(sql,
                transaction.getTransactionDate(),
                transaction.getAmount(),
                transaction.getId());
    }

    // Delete a transaction
    public int delete(int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
