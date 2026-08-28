package by.vs.erp.inventory.service;
import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.search.StockSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMigrationService {

    private final StockSearchRepository stockSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final StockMigrationTransactionalExecutor transactionalExecutor;

    @Async
    public void recreateIndexAndMigrate() {
        log.info("Запуск первоначальной миграции склада в Elasticsearch...");
        try {
            var indexOps = elasticsearchOperations.indexOps(StockIndexDocument.class);
            if (indexOps.exists()) {
                indexOps.delete();
                log.info("Старый индекс inventory_stocks успешно удален.");
            }
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping(StockIndexDocument.class));
            log.info("Новый индекс inventory_stocks инициализирован.");
        } catch (Exception e) {
            log.error("Критическая ошибка при инициализации индекса Elasticsearch: ", e);
            return;
        }

        int pageSize = 500;
        int pageNumber = 0;
        long totalMigrated = 0;

        Slice<Stock> stockSlice;
        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            stockSlice = transactionalExecutor.fetchPageAndClear(pageable);

            List<StockIndexDocument> documents = stockSlice.getContent().stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());

            if (!documents.isEmpty()) {
                stockSearchRepository.saveAll(documents);
                totalMigrated += documents.size();
                log.info("Мигрировано позиций: {}. Обработка продолжается...", totalMigrated);
            }

            pageNumber++;

        } while (stockSlice.hasNext());

        log.info("Миграция склада успешно завершена. Всего перенесено объектов: {}", totalMigrated);
    }

    private StockIndexDocument convertToDocument(Stock stock) {
        StockIndexDocument doc = new StockIndexDocument();
        doc.setId(String.valueOf(stock.getId()));
        doc.setQuantity(stock.getQuantity());
        doc.setPurchasePrice(stock.getPurchasePrice());
        doc.setRetailPrice(stock.getRetailPrice());
        if (stock.getPart() != null) {
            doc.setPartId(stock.getPart().getId());
            doc.setOemNumber(stock.getPart().getOemNumber());
            doc.setName(stock.getPart().getName());
            doc.setBrand(stock.getPart().getBrand());
        }
        return doc;
    }
}