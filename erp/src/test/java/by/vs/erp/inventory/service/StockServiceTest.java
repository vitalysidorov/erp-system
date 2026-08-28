package by.vs.erp.inventory.service;

import by.vs.erp.inventory.dto.StockDto;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.mapper.StockMapper;
import by.vs.erp.inventory.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("findAllWithParts: Успешное получение и маппинг всей страницы остатков склада")
    void shouldFindAllWithParts() {
        Pageable pageable = PageRequest.of(0, 20);
        Stock stock = new Stock();
        StockDto stockDto = new StockDto(1L, 10L, 5, null, null);

        Page<Stock> domainPage = new PageImpl<>(List.of(stock), pageable, 1);
        when(stockRepository.findAllWithParts(pageable)).thenReturn(domainPage);
        when(stockMapper.toDto(stock)).thenReturn(stockDto);

        Page<StockDto> result = stockService.findAllWithParts(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(5, result.getContent().get(0).getQuantity());
    }

    @Test
    @DisplayName("findByPartId: Возврат DTO при нахождении остатка по ID детали")
    void shouldFindByPartId_Success() {
        Long partId = 10L;
        Stock stock = new Stock();
        StockDto dto = new StockDto(1L, partId, 50, null, null);

        when(stockRepository.findByPartId(partId)).thenReturn(Optional.of(stock));
        when(stockMapper.toDto(stock)).thenReturn(dto);

        Optional<StockDto> result = stockService.findByPartId(partId);

        assertTrue(result.isPresent());
        assertEquals(50, result.get().getQuantity());
    }

    @Test
    @DisplayName("findByPartId: Выброс IllegalArgumentException, если позиция детали на складе отсутствует")
    void findByPartId_ThrowsException_WhenNotFound() {
        Long absentPartId = 999L;
        when(stockRepository.findByPartId(absentPartId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> stockService.findByPartId(absentPartId));
    }
}
