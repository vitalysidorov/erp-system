package by.vs.erp.finance.service;

import by.vs.erp.finance.dto.FinancialReportDto;
import by.vs.erp.finance.entity.Transaction;
import by.vs.erp.finance.entity.Type;
import by.vs.erp.finance.listener.FinanceEventListener;
import by.vs.erp.finance.repository.TransactionRepository;
import by.vs.erp.inventory.event.ReceivedNewBatchEvent;
import by.vs.erp.order.entity.WorkOrder;
import by.vs.erp.order.event.ClosedWorkOrderEvent;
import by.vs.erp.order.repository.WorkOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialAnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private FinancialAnalyticsService analyticsService;

    @InjectMocks
    private FinanceEventListener financeEventListener;

    @Test
    @DisplayName("Должен корректно рассчитать доходы, расходы и чистую прибыль за период")
    void shouldGenerateCorrectFinancialReport() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        Transaction income1 = new Transaction();
        income1.setType(Type.INCOME);
        income1.setAmount(new BigDecimal("1500.00"));

        Transaction income2 = new Transaction();
        income2.setType(Type.INCOME);
        income2.setAmount(new BigDecimal("500.00"));

        Transaction expense = new Transaction();
        expense.setType(Type.EXPENSE);
        expense.setAmount(new BigDecimal("800.00"));

        when(transactionRepository.findByCreatedAtBetween(start, end))
                .thenReturn(List.of(income1, income2, expense));
        when(workOrderRepository.countByStatus("IN_PROGRESS")).thenReturn(5L);

        FinancialReportDto report = analyticsService.generateReport(start, end);

        assertNotNull(report);
        assertEquals(new BigDecimal("2000.00"), report.getTotalIncome());
        assertEquals(new BigDecimal("800.00"), report.getTotalExpense());
        assertEquals(new BigDecimal("1200.00"), report.getNetProfit()); // 2000 - 800
        assertEquals(5L, report.getActiveOrdersCount());
    }

    @Test
    @DisplayName("Должен вернуть нулевой отчет, если за период транзакций не было")
    void shouldReturnZeroReportWhenNoTransactionsExist() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        when(transactionRepository.findByCreatedAtBetween(start, end)).thenReturn(List.of());
        when(workOrderRepository.countByStatus("IN_PROGRESS")).thenReturn(0L);

        FinancialReportDto report = analyticsService.generateReport(start, end);

        assertEquals(BigDecimal.ZERO, report.getTotalIncome());
        assertEquals(BigDecimal.ZERO, report.getTotalExpense());
        assertEquals(BigDecimal.ZERO, report.getNetProfit());
        assertEquals(0L, report.getActiveOrdersCount());
    }

    @Test
    @DisplayName("EventListener: Должен создать транзакцию расхода (EXPENSE) при принятии поставки")
    void handleReceivedNewBatchEvent_ShouldSaveExpenseTransaction() {
        ReceivedNewBatchEvent event = new ReceivedNewBatchEvent(new BigDecimal("1250.50"));

        financeEventListener.handleFinancialRecord(event);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());

        Transaction savedTransaction = captor.getValue();
        assertEquals(Type.EXPENSE, savedTransaction.getType());
        assertEquals(new BigDecimal("1250.50"), savedTransaction.getAmount());
    }

    @Test
    @DisplayName("EventListener: Должен создать транзакцию дохода (INCOME) при закрытии заказ-наряда")
    void handleClosedWorkOrderEvent_ShouldSaveIncomeTransaction() {
        WorkOrder mockOrder = new WorkOrder();
        ClosedWorkOrderEvent event = new ClosedWorkOrderEvent(mockOrder, new BigDecimal("5500.00"));

        financeEventListener.handleFinancialRecord(event);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());

        Transaction savedTransaction = captor.getValue();
        assertEquals(Type.INCOME, savedTransaction.getType());
        assertEquals(new BigDecimal("5500.00"), savedTransaction.getAmount());
        assertEquals(mockOrder, savedTransaction.getWorkOrder());
    }
}
