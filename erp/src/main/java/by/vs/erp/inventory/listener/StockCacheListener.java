package by.vs.erp.inventory.listener;

import by.vs.erp.common.util.BeanUtil;
import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.search.StockSearchRepository;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;

@Slf4j
public class StockCacheListener {

    @PostPersist
    public void onPostPersist(Stock stock) {
        syncWithElasticsearch(stock);
    }

    @PostUpdate
    public void onPostUpdate(Stock stock) {
        syncWithElasticsearch(stock);
        evictFromSpringCache(stock);
    }

    @PostRemove
    public void onPostRemove(Stock stock) {
        deleteFromElasticsearch(stock);
        evictFromSpringCache(stock);
    }

    private void syncWithElasticsearch(Stock stock) {
        try {
            StockSearchRepository searchRepository = BeanUtil.getBean(StockSearchRepository.class);
            StockIndexDocument doc = getStockIndexDocument(stock);
            searchRepository.save(doc);
            log.info("Stock с ID {} успешно синхронизирован с Elasticsearch", stock.getId());
        } catch (Exception e) {
            log.error("Ошибка синхронизации Stock с Elasticsearch: ", e);
        }
    }

    private void deleteFromElasticsearch(Stock stock) {
        try {
            StockSearchRepository searchRepository = BeanUtil.getBean(StockSearchRepository.class);
            searchRepository.deleteById(String.valueOf(stock.getId()));
            log.info("Stock с ID {} успешно удален из Elasticsearch", stock.getId());
        } catch (Exception e) {
            log.error("Ошибка удаления Stock из Elasticsearch: ", e);
        }
    }

    private void evictFromSpringCache(Stock stock) {
        try {
            if (stock.getPart() != null) {
                CacheManager cacheManager = BeanUtil.getBean(CacheManager.class);
                var cache = cacheManager.getCache("inv::stock");
                if (cache != null) {
                    cache.evict(stock.getPart().getId());
                    log.debug("Кэш 'inv::stock' успешно очищен для partId: {}", stock.getPart().getId());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка очистки Spring-кэша для Stock: ", e);
        }
    }

    private StockIndexDocument getStockIndexDocument(Stock stock) {
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
