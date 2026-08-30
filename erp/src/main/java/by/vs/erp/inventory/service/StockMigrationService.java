package by.vs.erp.inventory.service;

import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMigrationService {

    private static final String ALIAS_NAME = "inventory_stocks";

    private final ElasticsearchOperations elasticsearchOperations;
    private final StockMigrationTransactionalExecutor transactionalExecutor;


    @Async
    public void recreateIndexAndMigrate() {
        String newIndexName = ALIAS_NAME + "_v" + Instant.now().toEpochMilli();
        log.info("Запуск blue/green миграции склада в Elasticsearch. Новый индекс: {}", newIndexName);

        IndexCoordinates newIndexCoordinates = IndexCoordinates.of(newIndexName);
        IndexOperations newIndexOps = elasticsearchOperations.indexOps(newIndexCoordinates);

        try {
            newIndexOps.create();
            newIndexOps.putMapping(newIndexOps.createMapping(StockIndexDocument.class));
            log.info("Новый индекс {} создан и промаппирован.", newIndexName);
        } catch (Exception e) {
            log.error("Критическая ошибка при создании нового индекса {}: ", newIndexName, e);
            return;
        }

        long totalMigrated = migrateAllStocksInto(newIndexCoordinates);

        Set<String> oldIndices = resolveAliasIndices();
        switchAlias(newIndexOps, newIndexName, oldIndices);

        for (String oldIndex : oldIndices) {
            try {
                elasticsearchOperations.indexOps(IndexCoordinates.of(oldIndex)).delete();
                log.info("Старый индекс {} удалён после успешного переключения алиаса.", oldIndex);
            } catch (Exception e) {
                log.warn("Не удалось удалить старый индекс {} (не критично): {}", oldIndex, e.getMessage());
            }
        }

        log.info("Blue/green миграция завершена без простоя поиска. Перенесено позиций: {}", totalMigrated);
    }

    private long migrateAllStocksInto(IndexCoordinates targetIndex) {
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
                elasticsearchOperations.save(documents, targetIndex);
                totalMigrated += documents.size();
                log.info("Мигрировано позиций: {}. Обработка продолжается...", totalMigrated);
            }

            pageNumber++;
        } while (stockSlice.hasNext());

        return totalMigrated;
    }

    private Set<String> resolveAliasIndices() {
        try {
            IndexOperations aliasOps = elasticsearchOperations.indexOps(IndexCoordinates.of(ALIAS_NAME));
            return aliasOps.getAliases(ALIAS_NAME).keySet();
        } catch (Exception e) {
            log.info("Алиас {} ещё не существует — это первая миграция.", ALIAS_NAME);
            return Set.of();
        }
    }

    private void switchAlias(IndexOperations newIndexOps, String newIndexName, Set<String> oldIndices) {
        AliasActions actions = new AliasActions();

        for (String oldIndex : oldIndices) {
            actions.add(new AliasAction.Remove(
                    AliasActionParameters.builder()
                            .withIndices(oldIndex)
                            .withAliases(ALIAS_NAME)
                            .build()));
        }

        actions.add(new AliasAction.Add(
                AliasActionParameters.builder()
                        .withAliases(ALIAS_NAME)
                        .withIndices(newIndexName)
                        .build()));

        // старый индекс отвязывается и новый привязывается за одну операцию — момента "алиас никуда не указывает" нет.
        newIndexOps.alias(actions);
        log.info("Алиас {} переключён на индекс {}.", ALIAS_NAME, newIndexName);
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