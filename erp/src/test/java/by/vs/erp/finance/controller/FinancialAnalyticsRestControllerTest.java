package by.vs.erp.finance.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.finance.dto.FinancialReportDto;
import by.vs.erp.finance.service.FinancialAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinancialAnalyticsRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialAnalyticsService analyticsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Успешное получение финансового отчета администратором (проверка формата дат)")
    void shouldReturnReportWhenUserIsAdmin() throws Exception {
        FinancialReportDto expectedReport = FinancialReportDto.builder()
                .totalIncome(new BigDecimal("10000.00"))
                .totalExpense(new BigDecimal("4000.00"))
                .netProfit(new BigDecimal("6000.00"))
                .activeOrdersCount(3L)
                .build();

        Mockito.when(analyticsService.generateReport(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(expectedReport);

        String startIso = "2026-08-01T00:00:00";
        String endIso = "2026-08-24T23:59:59";

        mockMvc.perform(get("/api/v1/analytics/report")
                        .param("start", startIso)
                        .param("end", endIso)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(10000.00))
                .andExpect(jsonPath("$.totalExpense").value(4000.00))
                .andExpect(jsonPath("$.netProfit").value(6000.00))
                .andExpect(jsonPath("$.activeOrdersCount").value(3));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Отказ в доступе к финансовому отчету для роли MANAGER (403/500 в зависимости от вашего GlobalHandler)")
    void shouldDenyAccessWhenUserIsNotAdmin() throws Exception {
        String startIso = "2026-08-01T00:00:00";
        String endIso = "2026-08-24T23:59:59";

        mockMvc.perform(get("/api/v1/analytics/report")
                        .param("start", startIso)
                        .param("end", endIso))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Ошибка валидации 400 Bad Request при неверном формате даты")
    void shouldReturnBadRequestWhenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/report")
                        .param("start", "01-08-2026 00:00")
                        .param("end", "2026-08-24T23:59:59"))
                .andExpect(status().isBadRequest());
    }
}
