package by.vs.erp.inventory.controller;

import by.vs.erp.inventory.dto.PartBatchDto;
import by.vs.erp.inventory.dto.PartBatchReadDto;
import by.vs.erp.inventory.dto.PartCatalogReadDto;
import by.vs.erp.inventory.dto.StockDto;
import by.vs.erp.inventory.service.PartBatchService;
import by.vs.erp.inventory.service.PartCatalogService;
import by.vs.erp.inventory.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryRestController {

    private final PartCatalogService partCatalogService;
    private final StockService stockService;
    private final PartBatchService partBatchService;

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<Page<StockDto>> getAllStocks(@PageableDefault(size = 20) Pageable pageable) {
        log.info("API: Высокопроизводительный запрос всех остатков склада через JOIN FETCH");
        Page<StockDto> stocks = stockService.findAllWithParts(pageable);
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/parts/search")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<PartCatalogReadDto> findPartByOem(@RequestParam String oemNumber) {
        log.info("API: Поиск детали по OEM: {}", oemNumber);
        return partCatalogService.findByOemNumber(oemNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batches")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<PartBatchReadDto> receiveNewBatch(@Valid @RequestBody PartBatchDto batch,
                                                            @RequestParam BigDecimal amount) {
        log.info("API: Оформление поставки детали ID: {}, количество: {}, цена закупки: {}",
                batch.getPartId(), batch.getInitialQuantity(), batch.getPurchasePrice());
        PartBatchReadDto receivedBatch = partBatchService.receiveNewBatch(batch, amount);
        return new ResponseEntity<>(receivedBatch, HttpStatus.CREATED);
    }

    @GetMapping("/stocks/{partId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<StockDto> getPartStock(@PathVariable Long partId) {
        log.info("API: Запрос остатка детали ID: {} через JOIN FETCH", partId);
        return stockService.findByPartId(partId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stocks/deficient")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Slice<StockDto>> getDeficientParts(@RequestParam(defaultValue = "5") int minLimit,
                                                             @PageableDefault(size = 20) Pageable pageable) {
        log.info("API: Запрос списка дефицитных запчастей с порогом <= {} через JOIN FETCH", minLimit);
        Slice<StockDto> deficient = stockService.findDeficientParts(minLimit, pageable);
        return ResponseEntity.ok(deficient);
    }
}
