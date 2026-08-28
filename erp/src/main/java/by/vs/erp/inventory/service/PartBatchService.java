package by.vs.erp.inventory.service;

import by.vs.erp.inventory.dto.PartBatchDto;
import by.vs.erp.inventory.dto.PartBatchReadDto;
import by.vs.erp.inventory.entity.PartBatch;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.event.ReceivedNewBatchEvent;
import by.vs.erp.inventory.mapper.PartBatchMapper;
import by.vs.erp.inventory.repository.PartBatchRepository;
import by.vs.erp.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartBatchService {

    private final ApplicationEventPublisher eventPublisher;
    private final PartBatchRepository partBatchRepository;
    private final StockRepository stockRepository;
    private final PartBatchMapper partBatchMapper;

    @Value("${spring.application.inventory.default-markup}")
    private BigDecimal defaultMarkup;

    @Transactional
    public PartBatchReadDto receiveNewBatch(PartBatchDto batch, BigDecimal amount) {
        batch.setAvailableQuantity(batch.getInitialQuantity());
        PartBatch savedBatch = partBatchRepository.save(partBatchMapper.toEntity(batch));

        Stock stock = stockRepository.findByPartId(batch.getPartId())
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setPart(savedBatch.getPart());
                    newStock.setQuantity(0);
                    newStock.setRetailPrice(savedBatch.getPurchasePrice().multiply(defaultMarkup));
                    newStock.setPurchasePrice(savedBatch.getPurchasePrice());
                    return newStock;
                });

        stock.setQuantity(stock.getQuantity() + savedBatch.getInitialQuantity());
        stock.setPurchasePrice(savedBatch.getPurchasePrice());
        stockRepository.save(stock);

        eventPublisher.publishEvent(new ReceivedNewBatchEvent(amount));

        return partBatchMapper.toDto(savedBatch);
    }
}
