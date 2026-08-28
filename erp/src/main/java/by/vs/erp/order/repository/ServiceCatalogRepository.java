package by.vs.erp.order.repository;

import by.vs.erp.order.entity.ServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {
}
