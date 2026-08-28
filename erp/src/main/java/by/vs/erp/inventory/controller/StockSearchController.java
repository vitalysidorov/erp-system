package by.vs.erp.inventory.controller;

import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.service.StockMigrationService;
import by.vs.erp.inventory.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory/search")
@RequiredArgsConstructor
public class StockSearchController {

    private final StockSearchService stockSearchService;
    private final StockMigrationService stockMigrationService;

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerReindex() {
        stockMigrationService.recreateIndexAndMigrate();
        return ResponseEntity.ok("Процесс переиндексации склада запущен асинхронно в фоне");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<Page<StockIndexDocument>> searchStock(
            @RequestParam(name = "query") String query,
            @PageableDefault(size = 20,
                    sort = "quantity",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StockIndexDocument> searchResult = stockSearchService.search(query, pageable);
        return ResponseEntity.ok(searchResult);
    }
}
