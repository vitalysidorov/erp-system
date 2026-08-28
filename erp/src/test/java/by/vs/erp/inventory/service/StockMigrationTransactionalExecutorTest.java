package by.vs.erp.inventory.service;

import by.vs.erp.inventory.entity.PartCatalog;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.StockRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMigrationTransactionalExecutorTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private StockMigrationTransactionalExecutor transactionalExecutor;

    @Test
    @DisplayName("fetchPageAndClear: Загрузка порции данных, распаковка Lazy-связей и очистка контекста Hibernate")
    void shouldFetchPageAndClearEntityManager() {
        Pageable pageable = PageRequest.of(0, 10);

        PartCatalog partMock = mock(PartCatalog.class);
        Stock stock = new Stock();
        stock.setPart(partMock);

        Page<Stock> page = new PageImpl<>(List.of(stock));
        when(stockRepository.findAll(pageable)).thenReturn(page);

        Slice<Stock> result = transactionalExecutor.fetchPageAndClear(pageable);

        assertNotNull(result);
        verify(partMock, times(1)).getId(); // Проверяем, что прокси-коллекция была принудительно затриггерена
        verify(entityManager, times(1)).clear(); // Проверяем очистку контекста сессии L1
    }
}
