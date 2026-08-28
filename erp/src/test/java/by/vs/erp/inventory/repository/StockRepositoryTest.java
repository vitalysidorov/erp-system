package by.vs.erp.inventory.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.entity.PartCatalog;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.search.StockSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StockRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PartCatalogRepository partCatalogRepository;

    @Autowired
    private StockSearchRepository stockSearchRepository;

    @Test
    @DisplayName("PostPersist: Сохранение остатка в БД должно автоматически создавать документ в Elasticsearch через Listener")
    void shouldSyncWithElasticsearchOnSave() {
        PartCatalog part = new PartCatalog();
        part.setOemNumber("03L115562");
        part.setName("Фильтр масляный 2.0 TDI");
        part.setBrand("Knecht-Mahle");
        PartCatalog savedPart = partCatalogRepository.saveAndFlush(part);

        Stock stock = new Stock();
        stock.setPart(savedPart);
        stock.setQuantity(42);
        stock.setPurchasePrice(new BigDecimal("8.50"));
        stock.setRetailPrice(new BigDecimal("14.00"));

        Stock savedStock = stockRepository.saveAndFlush(stock);

        assertNotNull(savedStock.getId());

        Optional<StockIndexDocument> elasticDocOpt = stockSearchRepository.findById(String.valueOf(savedStock.getId()));

        assertTrue(elasticDocOpt.isPresent(), "Документ должен автоматически создаться в Elasticsearch!");
        StockIndexDocument doc = elasticDocOpt.get();
        assertEquals(42, doc.getQuantity());
        assertEquals("03L115562", doc.getOemNumber());
        assertEquals("Knecht-Mahle", doc.getBrand());
    }

    @Test
    @DisplayName("PostRemove: Удаление остатка из БД должно автоматически удалять его из Elasticsearch")
    void shouldDeleteFromElasticsearchOnRemove() {
        PartCatalog part = new PartCatalog();
        part.setOemNumber("W71294");
        part.setName("Фильтр масляный 1.4 TSI");
        part.setBrand("Mann");
        PartCatalog savedPart = partCatalogRepository.saveAndFlush(part);

        Stock stock = new Stock();
        stock.setPart(savedPart);
        stock.setQuantity(10);
        stock.setPurchasePrice(new BigDecimal("5.00"));
        stock.setRetailPrice(new BigDecimal("9.00"));

        Stock savedStock = stockRepository.saveAndFlush(stock);
        String docId = String.valueOf(savedStock.getId());

        assertTrue(stockSearchRepository.findById(docId).isPresent());

        stockRepository.delete(savedStock);
        stockRepository.flush();

        Optional<StockIndexDocument> deletedDocOpt = stockSearchRepository.findById(docId);
        assertTrue(deletedDocOpt.isEmpty(), "Документ должен быть удален из Elasticsearch");
    }

    @Test
    @DisplayName("findDeficientParts: Должен возвращать Slice деталей, чей остаток меньше или равен minLimit")
    void shouldFindDeficientParts() {
        PartCatalog part1 = createPart("OEM1", "Name1");
        PartCatalog part2 = createPart("OEM2", "Name2");

        createStock(part1, 3);
        createStock(part2, 10);

        stockRepository.flush();

        Slice<Stock> result = stockRepository.findDeficientParts(5, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(3, result.getContent().get(0).getQuantity());
    }

    @Test
    @DisplayName("searchFallback: Должен находить остатки по частичному совпадению имени или OEM без учета регистра")
    void shouldSearchFallbackByText() {
        PartCatalog part = createPart("06B103383H", "Прокладка ГБЦ Elring");
        createStock(part, 5);
        stockRepository.flush();

        Page<Stock> resultByOem = stockRepository.searchFallback("03383", PageRequest.of(0, 10));
        Page<Stock> resultByName = stockRepository.searchFallback("гбц", PageRequest.of(0, 10));

        assertFalse(resultByOem.isEmpty());
        assertFalse(resultByName.isEmpty());
        assertEquals("06B103383H", resultByOem.getContent().get(0).getPart().getOemNumber());
    }

    private PartCatalog createPart(String oem, String name) {
        PartCatalog part = new PartCatalog();
        part.setOemNumber(oem);
        part.setName(name);
        part.setBrand("TestBrand");
        return partCatalogRepository.saveAndFlush(part);
    }

    private void createStock(PartCatalog part, int qty) {
        Stock stock = new Stock();
        stock.setPart(part);
        stock.setQuantity(qty);
        stock.setPurchasePrice(BigDecimal.TEN);
        stock.setRetailPrice(BigDecimal.valueOf(15));
        stockRepository.save(stock);
    }
}
