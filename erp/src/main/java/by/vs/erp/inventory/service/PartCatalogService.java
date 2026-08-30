package by.vs.erp.inventory.service;

import by.vs.erp.common.exception.NotFoundException;
import by.vs.erp.inventory.dto.PartCatalogReadDto;
import by.vs.erp.inventory.mapper.PartCatalogMapper;
import by.vs.erp.inventory.repository.PartCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartCatalogService {

    private final PartCatalogRepository partCatalogRepository;
    private final PartCatalogMapper partCatalogMapper;

    @Cacheable(value = "inv::parts_catalog", key = "#oemNumber")
    @Transactional(readOnly = true)
    public Optional<PartCatalogReadDto> findByOemNumber(String oemNumber) {
        return Optional.of(partCatalogMapper.toDto(partCatalogRepository.findByOemNumber(oemNumber)
                .orElseThrow(() -> new NotFoundException("Деталь с артикулом " + oemNumber + " не найдена"))));
    }
}
