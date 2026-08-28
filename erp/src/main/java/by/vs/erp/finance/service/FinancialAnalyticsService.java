package by.vs.erp.finance.service;

import by.vs.erp.finance.dto.FinancialReportDto;
import by.vs.erp.finance.entity.Transaction;
import by.vs.erp.finance.entity.Type;
import by.vs.erp.finance.repository.TransactionRepository;
import by.vs.erp.order.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialAnalyticsService {

    private final TransactionRepository transactionRepository;
    private final WorkOrderRepository workOrderRepository;


    @Cacheable(value = "fin::dashboard", key = "{#start.hashCode(), #end.hashCode()}")
    @Transactional(readOnly = true)
    public FinancialReportDto generateReport(LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(start, end);

        Map<Type, BigDecimal> reportMap = transactions.stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getType() != null && t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getType,
                        Collectors.mapping(
                                Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        BigDecimal totalIncome = reportMap.getOrDefault(Type.INCOME, BigDecimal.ZERO);
        BigDecimal totalExpense = reportMap.getOrDefault(Type.EXPENSE, BigDecimal.ZERO);

        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        long activeOrders = workOrderRepository.countByStatus("IN_PROGRESS");

        return FinancialReportDto.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .activeOrdersCount(activeOrders)
                .build();
    }
}
