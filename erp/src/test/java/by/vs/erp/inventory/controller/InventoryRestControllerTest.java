package by.vs.erp.inventory.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.inventory.dto.*;
import by.vs.erp.inventory.service.PartBatchService;
import by.vs.erp.inventory.service.PartCatalogService;
import by.vs.erp.inventory.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PartCatalogService partCatalogService;

    @MockBean
    private StockService stockService;

    @MockBean
    private PartBatchService partBatchService;

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("GET /stocks: Должен вернуть страницу остатков склада для роли MASTER")
    void shouldReturnAllStocksWhenUserIsMaster() throws Exception {
        StockDto stockDto = new StockDto(1L, 100L, 15, new BigDecimal("50.00"), new BigDecimal("75.00"));
        PageImpl<StockDto> page = new PageImpl<>(List.of(stockDto), PageRequest.of(0, 20), 1);

        Mockito.when(stockService.findAllWithParts(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/inventory/stocks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].quantity").value(15));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("GET /parts/search: Должен успешно найти запчасть в каталоге по OEM")
    void shouldFindPartByOem() throws Exception {
        PartCatalogReadDto part = new PartCatalogReadDto(100L, "06B103383H", "Прокладка ГБЦ", "Elring");

        Mockito.when(partCatalogService.findByOemNumber("06B103383H")).thenReturn(Optional.of(part));

        mockMvc.perform(get("/api/v1/inventory/parts/search")
                        .param("oemNumber", "06B103383H"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.name").value("Прокладка ГБЦ"))
                .andExpect(jsonPath("$.brand").value("Elring"));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("GET /parts/search: Должен вернуть 404 Not Found, если деталь не найдена по OEM")
    void shouldReturn404WhenPartNotFoundByOem() throws Exception {
        Mockito.when(partCatalogService.findByOemNumber("ABSENT")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/inventory/parts/search")
                        .param("oemNumber", "ABSENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /batches: Успешное оформление новой поставки роли MANAGER")
    void shouldReceiveNewBatchWhenUserIsManager() throws Exception {
        // Создаем тестовые данные (используем валидные значения для прохождения тестов)
        PartBatchDto inputDto = new PartBatchDto(100L, 50, 50, new BigDecimal("12.50"));
        PartBatchReadDto responseDto = new PartBatchReadDto(1L, 100L, 50, 50, new BigDecimal("12.50"), LocalDateTime.now());

        Mockito.when(partBatchService.receiveNewBatch(any(PartBatchDto.class), eq(new BigDecimal("625.00"))))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/inventory/batches")
                        .param("amount", "625.00")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.availableQuantity").value(50));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("POST /batches: Роль MASTER должна получить отказ доступа 403 при попытке оприходовать поставку")
    void shouldReturnForbiddenWhenMasterTriesToReceiveBatch() throws Exception {
        PartBatchDto inputDto = new PartBatchDto(100L, 50, 50, new BigDecimal("12.50"));

        mockMvc.perform(post("/api/v1/inventory/batches")
                        .param("amount", "625.00")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /stocks/deficient: Успешное получение списка дефицитных позиций")
    void shouldGetDeficientParts() throws Exception {
        StockDto stockDto = new StockDto(2L, 105L, 2, new BigDecimal("10.00"), new BigDecimal("15.00"));
        SliceImpl<StockDto> slice = new SliceImpl<>(List.of(stockDto), PageRequest.of(0, 20), false);

        Mockito.when(stockService.findDeficientParts(eq(5), any())).thenReturn(slice);

        mockMvc.perform(get("/api/v1/inventory/stocks/deficient")
                        .param("minLimit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].partId").value(105L))
                .andExpect(jsonPath("$.content[0].quantity").value(2));
    }
}
