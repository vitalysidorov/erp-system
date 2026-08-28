package by.vs.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MechanicTaskDto {
    private Long orderId;
    private Long taskId;
    private String carInfo;
    private String serviceName;
    private BigDecimal normHours;
}
