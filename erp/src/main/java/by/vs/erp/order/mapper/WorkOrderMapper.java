package by.vs.erp.order.mapper;

import by.vs.erp.crm.entity.Client;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.order.dto.WorkOrderResponseDto;
import by.vs.erp.order.entity.OrderPartItem;
import by.vs.erp.order.entity.OrderServiceItem;
import by.vs.erp.order.entity.WorkOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {BookingMapper.class})
public interface WorkOrderMapper {

    @Mapping(target = "clientFullName", source = "vehicle.client", qualifiedByName = "mapClientName")
    @Mapping(target = "carDetails", source = "vehicle") // Обработается через BookingMapper.mapCarInfo
    @Mapping(target = "vin", source = "vehicle.vin")
    @Mapping(target = "totalAmount", source = "entity", qualifiedByName = "calculateTotal")
    WorkOrderResponseDto toResponseDto(WorkOrder entity);

    @Mapping(target = "serviceName", source = "service.name")
    @Mapping(target = "mechanicName", source = "mechanic", qualifiedByName = "mapEmployeeName")
    WorkOrderResponseDto.ServiceItemDto toServiceItemDto(OrderServiceItem entity);

    @Mapping(target = "partName", source = "part.name")
    @Mapping(target = "oemNumber", source = "part.oemNumber")
    WorkOrderResponseDto.PartItemDto toPartItemDto(OrderPartItem entity);

    @Named("mapClientName")
    default String mapClientName(Client client) {
        if (client == null) return "";
        return client.getLastName() + " " + client.getFirstName();
    }

    @Named("mapEmployeeName")
    default String mapEmployeeName(Employee employee) {
        if (employee == null) return "Не назначен";
        return employee.getLastName() + " " + employee.getFirstName().substring(0, 1) + ".";
    }

    @Named("calculateTotal")
    default BigDecimal calculateTotal(WorkOrder order) {
        if (order == null) return BigDecimal.ZERO;

        BigDecimal servicesSum = order.getServices().stream()
                .map(s -> s.getFinalPrice().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal partsSum = order.getParts().stream()
                .map(p -> p.getFinalPrice().multiply(BigDecimal.valueOf(p.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return servicesSum.add(partsSum);
    }
}

