package by.vs.erp.inventory.service;

import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.entity.PartCatalog;
import by.vs.erp.inventory.entity.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMigrationServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private StockMigrationTransactionalExecutor transactionalExecutor;

    @InjectMocks
    private StockMigrationService stockMigrationService;

    @Test
    @DisplayName("recreateIndexAndMigrate: Blue/green — создаёт новый индекс, мигрирует данные напрямую в него, " +
            "атомарно переключает алиас и удаляет старый индекс, не трогая поиск во время миграции")
    void shouldMigrateUsingBlueGreenAliasSwap() {
        IndexOperations newIndexOps = mock(IndexOperations.class);
        when(newIndexOps.create()).thenReturn(true);
        when(newIndexOps.createMapping(StockIndexDocument.class)).thenReturn(null);
        when(newIndexOps.putMapping((Document) any())).thenReturn(true);
        when(newIndexOps.alias(any(AliasActions.class))).thenReturn(true);

        // Алиас "inventory_stocks" сейчас указывает на старый физический индекс
        IndexOperations aliasOps = mock(IndexOperations.class);
        when(aliasOps.getAliases("inventory_stocks"))
                .thenReturn(Map.of("inventory_stocks_v_old", Set.of()));

        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
                .thenAnswer(invocation -> {
                    IndexCoordinates coords = invocation.getArgument(0);
                    return "inventory_stocks".equals(coords.getIndexName()) ? aliasOps : newIndexOps;
                });

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

        // Новый индекс создаётся и маппится
        verify(newIndexOps, times(1)).create();
        verify(newIndexOps, times(1)).putMapping((Document) any());

        // Данные пишутся НАПРЯМУЮ в новый версионный индекс, минуя алиас/репозиторий —
        // иначе поиск был бы недоступен во время миграции
        verify(elasticsearchOperations, times(1)).save(anyList(), any(IndexCoordinates.class));

        // Алиас переключается одним атомарным вызовом
        ArgumentCaptor<AliasActions> aliasCaptor = ArgumentCaptor.forClass(AliasActions.class);
        verify(newIndexOps, times(1)).alias(aliasCaptor.capture());

        // Старый индекс, на который раньше указывал алиас, удаляется после переключения
        verify(elasticsearchOperations, times(1))
                .indexOps(eq(IndexCoordinates.of("inventory_stocks_v_old")));
    }

    @Test
    @DisplayName("recreateIndexAndMigrate: Первая миграция (алиас ещё не существует) не должна падать " +
            "и всё равно создаёт индекс и переключает на него алиас")
    void shouldHandleFirstMigrationWithoutExistingAlias() {
        IndexOperations newIndexOps = mock(IndexOperations.class);
        when(newIndexOps.create()).thenReturn(true);
        when(newIndexOps.createMapping(StockIndexDocument.class)).thenReturn(null);
        when(newIndexOps.putMapping((Document) any())).thenReturn(true);
        when(newIndexOps.alias(any(AliasActions.class))).thenReturn(true);

        IndexOperations aliasOps = mock(IndexOperations.class);
        when(aliasOps.getAliases("inventory_stocks"))
                .thenThrow(new RuntimeException("index_not_found_exception"));

        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
                .thenAnswer(invocation -> {
                    IndexCoordinates coords = invocation.getArgument(0);
                    return "inventory_stocks".equals(coords.getIndexName()) ? aliasOps : newIndexOps;
                });

        when(transactionalExecutor.fetchPageAndClear(any())).thenReturn(new SliceImpl<>(List.of()));

        stockMigrationService.recreateIndexAndMigrate();

        verify(newIndexOps, times(1)).create();
        verify(newIndexOps, times(1)).alias(any(AliasActions.class));
        // Удалять нечего — старых физических индексов не было
        verify(elasticsearchOperations, never()).indexOps(eq(IndexCoordinates.of("inventory_stocks")))
                .delete();
    }
}