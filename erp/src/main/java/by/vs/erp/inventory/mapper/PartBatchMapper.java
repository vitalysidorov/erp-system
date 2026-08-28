package by.vs.erp.inventory.mapper;

import by.vs.erp.inventory.dto.PartBatchDto;
import by.vs.erp.inventory.dto.PartBatchReadDto;
import by.vs.erp.inventory.entity.PartBatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PartBatchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    @Mapping(target = "part.id", source = "partId")
    PartBatch toEntity(PartBatchDto partBatchDto);

    @Mapping(target = "partId", source = "part.id")
    PartBatchReadDto toDto(PartBatch partBatch);
}
