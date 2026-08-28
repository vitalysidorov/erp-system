package by.vs.erp.inventory.service;

import by.vs.erp.inventory.dto.StockDto;
import by.vs.erp.inventory.mapper.StockMapper;
import by.vs.erp.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final StockMapper stockMapper;

    @Transactional(readOnly = true)
    public Page<StockDto> findAllWithParts(Pageable pageable) {
        return stockRepository.findAllWithParts(pageable).map(stockMapper::toDto);
    }

    @Cacheable(value = "inv::stock", key = "#partId")
    @Transactional(readOnly = true)
    public Optional<StockDto> findByPartId(Long partId) {
        return Optional.of(stockMapper.toDto(stockRepository.findByPartId(partId)
                .orElseThrow(() -> new IllegalArgumentException("Позиция детали с id: " + partId + " не найдена на складе"))));
    }

    @Transactional(readOnly = true)
    public Slice<StockDto> findDeficientParts(int minLimit, Pageable pageable) {
        return stockRepository.findDeficientParts(minLimit, pageable).map(stockMapper::toDto);
    }
}
