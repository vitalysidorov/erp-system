package by.vs.erp.finance.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.finance.entity.Transaction;
import by.vs.erp.finance.entity.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Должен сохранять транзакции и автоматически генерировать дату createdAt через @PrePersist")
    void shouldSaveTransactionWithPrePersistTime() {
        Transaction transaction = new Transaction();
        transaction.setType(Type.INCOME);
        transaction.setAmount(new BigDecimal("350.00"));

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt(), "Дата создания должна сгенерироваться автоматически");
    }

    @Test
    @DisplayName("Должен находить транзакции строго внутри указанного временного интервала")
    void shouldFindTransactionsBetweenDates() throws InterruptedException {
        LocalDateTime start = LocalDateTime.now();

        Transaction t1 = new Transaction();
        t1.setType(Type.INCOME);
        t1.setAmount(new BigDecimal("100.00"));
        transactionRepository.save(t1);

        Thread.sleep(10);
        LocalDateTime end = LocalDateTime.now();
        Thread.sleep(10);

        Transaction t2 = new Transaction();
        t2.setType(Type.EXPENSE);
        t2.setAmount(new BigDecimal("50.00"));
        transactionRepository.save(t2);

        List<Transaction> result = transactionRepository.findByCreatedAtBetween(start, end);

        assertEquals(1, result.size(), "Должна быть найдена только транзакция t1");
        assertEquals(new BigDecimal("100.00"), result.get(0).getAmount());
    }
}
