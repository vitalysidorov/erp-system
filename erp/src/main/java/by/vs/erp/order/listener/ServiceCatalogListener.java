package by.vs.erp.order.listener;

import by.vs.erp.common.util.BeanUtil;
import by.vs.erp.order.entity.ServiceCatalog;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.cache.CacheManager;

public class ServiceCatalogListener {

    @PostUpdate
    @PostRemove
    public void evictStockCache(ServiceCatalog serviceCatalog) {
        CacheManager cacheManager = BeanUtil.getBean(CacheManager.class);
        if (cacheManager != null) {
            var cache = cacheManager.getCache("ord::services_catalog");
            if (cache != null) {
                cache.evict(serviceCatalog.getId());
            }
        }
    }
}
