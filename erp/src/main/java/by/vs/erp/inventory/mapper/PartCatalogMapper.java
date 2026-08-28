package by.vs.erp.inventory.mapper;

import by.vs.erp.inventory.dto.PartCatalogReadDto;
import by.vs.erp.inventory.entity.PartCatalog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PartCatalogMapper {
    PartCatalogReadDto toDto(PartCatalog partCatalog);
}
