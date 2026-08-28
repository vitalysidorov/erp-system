package by.vs.erp.order.mapper;

import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.order.dto.BookingRequestDto;
import by.vs.erp.order.dto.BookingResponseDto;
import by.vs.erp.order.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "idToVehicle")
    Booking toEntity(BookingRequestDto dto);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    BookingResponseDto toDto(Booking entity);

    @Named("idToVehicle")
    default Vehicle idToVehicle(Long vehicleId) {
        if (vehicleId == null) return null;
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        return vehicle;
    }

    default String mapCarInfo(Vehicle vehicle) {
        if (vehicle == null) return "";
        return String.format("%s %s (%s)",
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getPlateNumber() != null ? vehicle.getPlateNumber() : "Без г/н"
        );
    }
}

