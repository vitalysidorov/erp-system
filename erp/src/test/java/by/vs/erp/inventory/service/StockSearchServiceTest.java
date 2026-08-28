package by.vs.erp.inventory.service;

import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.entity.PartCatalog;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockSearchService stockSearchService;

    @Test
    @DisplayName("search: Успешный поиск через Elasticsearch и маппинг результатов")
    @SuppressWarnings("unchecked")
    void shouldSearchInElasticsearchSuccessfully() {
        String searchText = "Тормозные диски";
        Pageable pageable = PageRequest.of(0, 20);

        StockIndexDocument doc = new StockIndexDocument();
        doc.setId("1");
        doc.setName("Диск тормозной передний");
        doc.setOemNumber("09914511");

        SearchHit<StockIndexDocument> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(doc);

        SearchHits<StockIndexDocument> searchHits = mock(SearchHits.class);
        when(searchHits.get()).thenReturn(Stream.of(searchHit));
        when(searchHits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(StockIndexDocument.class)))
                .thenReturn(searchHits);

        Page<StockIndexDocument> result = stockSearchService.search(searchText, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("09914511", result.getContent().get(0).getOemNumber());
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("search: Переключение на резервный поиск СУБД (Database Fallback) при сбое Elasticsearch")
    void shouldFallbackToDatabaseWhenElasticsearchFails() {
        String searchText = "Амортизатор";
        Pageable pageable = PageRequest.of(0, 20);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(StockIndexDocument.class)))
                .thenThrow(new RuntimeException("Elasticsearch cluster unavailable"));

        PartCatalog part = new PartCatalog();
        part.setOemNumber("314881");
        part.setName("Амортизатор задний");
        part.setBrand("Sachs");

        Stock stock = new Stock();
        stock.setId(15L);
        stock.setPart(part);
        stock.setQuantity(4);

        Page<Stock> jpaPage = new PageImpl<>(List.of(stock), pageable, 1);
        when(stockRepository.searchFallback(searchText, pageable)).thenReturn(jpaPage);

        Page<StockIndexDocument> result = stockSearchService.search(searchText, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("314881", result.getContent().get(0).getOemNumber());
        assertEquals("Sachs", result.getContent().get(0).getBrand());

        verify(stockRepository, times(1)).searchFallback(searchText, pageable);
    }

    @Test
    @DisplayName("search: Возврат пустой страницы при передаче пустого или null запроса")
    void shouldReturnEmptyPageWhenQueryIsEmpty() {
        Page<StockIndexDocument> resultNull = stockSearchService.search(null, PageRequest.of(0, 10));
        Page<StockIndexDocument> resultBlank = stockSearchService.search("   ", PageRequest.of(0, 10));

        assertTrue(resultNull.isEmpty());
        assertTrue(resultBlank.isEmpty());
        verifyNoInteractions(elasticsearchOperations);
        verifyNoInteractions(stockRepository);
    }
}
