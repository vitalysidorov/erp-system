package by.vs.erp.inventory.service;

import by.vs.erp.inventory.dto.PartBatchDto;
import by.vs.erp.inventory.dto.PartBatchReadDto;
import by.vs.erp.inventory.entity.PartBatch;
import by.vs.erp.inventory.entity.PartCatalog;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.event.ReceivedNewBatchEvent;
import by.vs.erp.inventory.mapper.PartBatchMapper;
import by.vs.erp.inventory.repository.PartBatchRepository;
import by.vs.erp.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartBatchServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PartBatchRepository partBatchRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private PartBatchMapper partBatchMapper;

    @InjectMocks
    private PartBatchService partBatchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(partBatchService, "defaultMarkup", new BigDecimal("1.50"));
    }

    @Test
    @DisplayName("receiveNewBatch: Успешное добавление партии для нового товара (расчет розничной цены по умолчанию)")
    void shouldCreateNewStockWhenPartIsNew() {
        PartBatchDto dto = new PartBatchDto(1L, 10, 10, new BigDecimal("100.00"));

        PartCatalog part = new PartCatalog();
        part.setId(1L);

        PartBatch entity = new PartBatch();
        entity.setPart(part);
        entity.setInitialQuantity(10);
        entity.setPurchasePrice(new BigDecimal("100.00"));

        when(partBatchMapper.toEntity(dto)).thenReturn(entity);
        when(partBatchRepository.save(entity)).thenReturn(entity);
        when(stockRepository.findByPartId(1L)).thenReturn(Optional.empty()); // Склад пуст
        when(partBatchMapper.toDto(entity)).thenReturn(new PartBatchReadDto(77L, 1L, 10, 10, new BigDecimal("100.00"), null));

        PartBatchReadDto result = partBatchService.receiveNewBatch(dto, new BigDecimal("1000.00"));

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository, times(1)).save(stockCaptor.capture());

        Stock savedStock = stockCaptor.getValue();
        assertEquals(10, savedStock.getQuantity());
        assertEquals(0, new BigDecimal("150.00").compareTo(savedStock.getRetailPrice()),
                "Розничная цена должна быть математически равна 150.00");
        assertEquals(new BigDecimal("100.00"), savedStock.getPurchasePrice());

        verify(eventPublisher, times(1)).publishEvent(any(ReceivedNewBatchEvent.class));
        assertNotNull(result);
    }

    @Test
    @DisplayName("receiveNewBatch: Обновление существующего остатка товара на складе")
    void shouldUpdateExistingStockWhenPartExists() {
        PartBatchDto dto = new PartBatchDto(1L, 5, 5, new BigDecimal("120.00"));

        PartCatalog part = new PartCatalog();
        part.setId(1L);

        PartBatch entity = new PartBatch();
        entity.setPart(part);
        entity.setInitialQuantity(5);
        entity.setPurchasePrice(new BigDecimal("120.00"));

        Stock existingStock = new Stock();
        existingStock.setPart(part);
        existingStock.setQuantity(20); // Исходно было 20 штук
        existingStock.setPurchasePrice(new BigDecimal("100.00"));
        existingStock.setRetailPrice(new BigDecimal("150.00"));

        when(partBatchMapper.toEntity(dto)).thenReturn(entity);
        when(partBatchRepository.save(entity)).thenReturn(entity);
        when(stockRepository.findByPartId(1L)).thenReturn(Optional.of(existingStock));

        partBatchService.receiveNewBatch(dto, new BigDecimal("600.00"));

        verify(stockRepository, times(1)).save(existingStock);
        assertEquals(25, existingStock.getQuantity(), "Новое количество должно стать 20 + 5 = 25");
        assertEquals(new BigDecimal("120.00"), existingStock.getPurchasePrice(), "Цена закупки должна обновиться на актуальную из последней поставки");
    }
}
