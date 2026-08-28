package by.vs.erp.order.mapper;

import by.vs.erp.order.dto.MechanicTaskDto;
import by.vs.erp.order.entity.OrderServiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookingMapper.class})
public interface MechanicTaskMapper {

    @Mapping(target = "orderId", source = "workOrder.id")
    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "carInfo", source = "workOrder.vehicle")
    @Mapping(target = "serviceName", source = "service.name")
    @Mapping(target = "normHours", source = "service.normHours")
    MechanicTaskDto toTaskDto(OrderServiceItem entity);
}

