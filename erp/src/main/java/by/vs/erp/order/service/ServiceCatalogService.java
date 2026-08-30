package by.vs.erp.order.service;

import by.vs.erp.common.exception.NotFoundException;
import by.vs.erp.order.entity.ServiceCatalog;
import by.vs.erp.order.repository.ServiceCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceCatalogService {

    private final ServiceCatalogRepository serviceCatalogRepository;

    @Cacheable(value = "ord::services_catalog", key = "#serviceId")
    @Transactional(readOnly = true)
    public ServiceCatalog findById(Long id) {
        return serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Услуга не найдена в каталоге"));
    }
}
