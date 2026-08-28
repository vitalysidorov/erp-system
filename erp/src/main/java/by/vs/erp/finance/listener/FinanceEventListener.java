package by.vs.erp.finance.listener;

import by.vs.erp.finance.entity.Transaction;
import by.vs.erp.finance.repository.TransactionRepository;
import by.vs.erp.inventory.event.ReceivedNewBatchEvent;
import by.vs.erp.order.event.ClosedWorkOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static by.vs.erp.finance.entity.Type.EXPENSE;
import static by.vs.erp.finance.entity.Type.INCOME;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceEventListener {

    private final TransactionRepository transactionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleFinancialRecord(ReceivedNewBatchEvent event) {
        log.info("FinancialRecord: Создание транзакции после принятия поставки детали");

        Transaction transaction = new Transaction();
        transaction.setType(EXPENSE);
        transaction.setAmount(event.amount());
        transactionRepository.save(transaction);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleFinancialRecord(ClosedWorkOrderEvent event) {
        log.info("FinancialRecord: Создание транзакции после закрытия заказ-наряда");

        Transaction transaction = new Transaction();
        transaction.setWorkOrder(event.order());
        transaction.setType(INCOME);
        transaction.setAmount(event.finalRevenue());
        transactionRepository.save(transaction);
    }
}
