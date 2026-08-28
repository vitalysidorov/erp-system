package by.vs.erp.inventory.service;

import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.StockRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockMigrationTransactionalExecutor {

    private final StockRepository stockRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Slice<Stock> fetchPageAndClear(Pageable pageable) {
        Slice<Stock> slice = stockRepository.findAll(pageable);
        slice.getContent().forEach(stock -> {
            if (stock.getPart() != null) {
                stock.getPart().getId();
            }
        });

        entityManager.clear();

        return slice;
    }
}