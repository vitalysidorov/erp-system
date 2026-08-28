package by.vs.erp.inventory.service;

import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.entity.PartCatalog;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.search.StockSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMigrationServiceTest {

    @Mock
    private StockSearchRepository stockSearchRepository;
    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private StockMigrationTransactionalExecutor transactionalExecutor;

    @InjectMocks
    private StockMigrationService stockMigrationService;

    @Test
    @DisplayName("recreateIndexAndMigrate: Должен пересоздать индекс и мигрировать все пачки документов")
    void shouldRecreateIndexAndMigrateData() {
        IndexOperations mockIndexOps = mock(IndexOperations.class);
        when(elasticsearchOperations.indexOps(StockIndexDocument.class)).thenReturn(mockIndexOps);
        when(mockIndexOps.exists()).thenReturn(true);

        PartCatalog part = new PartCatalog();
        part.setId(10L);
        part.setName("Свеча зажигания");
        part.setOemNumber("12345");
        part.setBrand("NGK");

        Stock stock = new Stock();
        stock.setId(1L);
        stock.setPart(part);
        stock.setQuantity(100);
        stock.setPurchasePrice(BigDecimal.ONE);
        stock.setRetailPrice(BigDecimal.TEN);

        Slice<Stock> stockSlice = new SliceImpl<>(List.of(stock));
        when(transactionalExecutor.fetchPageAndClear(any())).thenReturn(stockSlice);

        stockMigrationService.recreateIndexAndMigrate();

        verify(mockIndexOps, times(1)).delete(); // Проверка удаления старого индекса
        verify(mockIndexOps, times(1)).create(); // Проверка создания нового индекса
        verify(stockSearchRepository, times(1)).saveAll(anyList()); // Проверка сохранения мигрированных документов
    }
}
