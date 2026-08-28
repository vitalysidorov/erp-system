package by.vs.erp.inventory.repository;

import by.vs.erp.inventory.entity.PartCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartCatalogRepository extends JpaRepository<PartCatalog, Long> {
    Optional<PartCatalog> findByOemNumber(String oemNumber);
}
