package by.vs.erp.inventory.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.inventory.entity.PartBatch;
import by.vs.erp.inventory.entity.PartCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartBatchRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private PartBatchRepository partBatchRepository;

    @Autowired
    private PartCatalogRepository partCatalogRepository;

    @Test
    @DisplayName("findAvailableBatches: Должен возвращать партии с доступным остатком в порядке FIFO")
    void shouldFindAvailableBatchesInFifoOrder() throws InterruptedException {
        PartCatalog part = new PartCatalog();
        part.setOemNumber("5Q0129620B");
        part.setName("Фильтр воздушный");
        part.setBrand("VAG");
        PartCatalog savedPart = partCatalogRepository.saveAndFlush(part);

        PartBatch batch1 = new PartBatch();
        batch1.setPart(savedPart);
        batch1.setInitialQuantity(10);
        batch1.setAvailableQuantity(5);
        batch1.setPurchasePrice(new BigDecimal("15.00"));
        batch1.setReceivedAt(LocalDateTime.now().minusDays(2));
        partBatchRepository.save(batch1);

        PartBatch batch2 = new PartBatch();
        batch2.setPart(savedPart);
        batch2.setInitialQuantity(20);
        batch2.setAvailableQuantity(20);
        batch2.setPurchasePrice(new BigDecimal("17.00"));
        batch2.setReceivedAt(LocalDateTime.now().minusDays(1));
        partBatchRepository.save(batch2);

        PartBatch batch3 = new PartBatch();
        batch3.setPart(savedPart);
        batch3.setInitialQuantity(5);
        batch3.setAvailableQuantity(0); // Остаток 0!
        batch3.setPurchasePrice(new BigDecimal("12.00"));
        batch3.setReceivedAt(LocalDateTime.now().minusDays(5));
        partBatchRepository.save(batch3);

        partBatchRepository.flush();

        List<PartBatch> availableBatches = partBatchRepository.findAvailableBatches(savedPart.getId());

        assertEquals(2, availableBatches.size(), "Должны вернуться только партии с остатком > 0");
        assertEquals(5, availableBatches.get(0).getAvailableQuantity());
        assertEquals(new BigDecimal("15.00"), availableBatches.get(0).getPurchasePrice(), "Первой по списку (FIFO) должна идти более старая партия");
        assertEquals(new BigDecimal("17.00"), availableBatches.get(1).getPurchasePrice());
    }
}
