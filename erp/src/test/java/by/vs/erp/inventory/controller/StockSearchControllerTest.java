package by.vs.erp.inventory.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.service.StockMigrationService;
import by.vs.erp.inventory.service.StockSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StockSearchControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockSearchService stockSearchService;

    @MockBean
    private StockMigrationService stockMigrationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /reindex: Успешный запуск переиндексации администратором")
    void shouldTriggerReindexWhenUserIsAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/search/reindex"))
                .andExpect(status().isOk())
                .andExpect(content().string("Процесс переиндексации склада запущен асинхронно в фоне"));

        Mockito.verify(stockMigrationService, Mockito.times(1)).recreateIndexAndMigrate();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /reindex: Роль MANAGER должна получить отказ 403 при попытке вызвать реиндексацию")
    void shouldDenyReindexWhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/search/reindex"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("GET /search: Выполнение полнотекстового поиска по складу для MASTER/MANAGER")
    void shouldSearchStockSuccessfully() throws Exception {
        StockIndexDocument document = new StockIndexDocument();
        document.setId("1");
        document.setName("Фильтр воздушный");
        document.setOemNumber("MANN-C30005");
        document.setQuantity(25);

        PageImpl<StockIndexDocument> resultPage = new PageImpl<>(List.of(document), PageRequest.of(0, 20), 1);

        Mockito.when(stockSearchService.search(eq("Фильтр"), any())).thenReturn(resultPage);

        mockMvc.perform(get("/api/v1/inventory/search")
                        .param("query", "Фильтр")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Фильтр воздушный"))
                .andExpect(jsonPath("$.content[0].oemNumber").value("MANN-C30005"))
                .andExpect(jsonPath("$.content[0].quantity").value(25));
    }
}
