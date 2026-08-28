package by.vs.erp.crm.mapper;

import by.vs.erp.crm.dto.VehicleDto;
import by.vs.erp.crm.dto.VehicleReadDto;
import by.vs.erp.crm.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "clientId", target = "client.id")
    Vehicle toEntity(VehicleDto vehicleDto);

    @Mapping(source = "client.id", target = "clientId")
    VehicleReadDto toDto(Vehicle vehicle);
}
