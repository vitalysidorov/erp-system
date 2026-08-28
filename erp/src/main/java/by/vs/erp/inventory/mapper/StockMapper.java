package by.vs.erp.inventory.mapper;

import by.vs.erp.inventory.dto.StockDto;
import by.vs.erp.inventory.entity.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "partId", target = "part.id")
    Stock toEntity(StockDto stockDto);

    @Mapping(source = "part.id", target = "partId")
    StockDto toDto(Stock stock);
}
