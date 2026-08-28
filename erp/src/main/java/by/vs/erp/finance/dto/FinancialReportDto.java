package by.vs.erp.finance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class FinancialReportDto {
    private BigDecimal totalIncome;    // закрытые ремонты
    private BigDecimal totalExpense;   // cумма расходов
    private BigDecimal netProfit;      // прибыль
    private Long activeOrdersCount;    // cколько машин сейчас в ремзоне
}

