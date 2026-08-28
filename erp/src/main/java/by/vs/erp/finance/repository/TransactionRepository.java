package by.vs.erp.finance.repository;

import by.vs.erp.finance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

