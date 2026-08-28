package by.vs.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderResponseDto {
    private Long id;
    private String clientFullName;
    private String carDetails;
    private String vin;
    private String status;
    private Integer mileageIn;
    private String fuelLevel;
    private String damagesNotes;
    private LocalDateTime createdAt;
    private List<ServiceItemDto> services;
    private List<PartItemDto> parts;
    private BigDecimal totalAmount;

    public WorkOrderResponseDto(Long id, String clientFullName, String carDetails, String vin, String status,
                                Integer mileageIn, String fuelLevel, String damagesNotes, LocalDateTime createdAt,
                                List<ServiceItemDto> services, List<PartItemDto> parts) {
        this.id = id;
        this.clientFullName = clientFullName;
        this.carDetails = carDetails;
        this.vin = vin;
        this.status = status;
        this.mileageIn = mileageIn;
        this.fuelLevel = fuelLevel;
        this.damagesNotes = damagesNotes;
        this.createdAt = createdAt;
        this.services = services;
        this.parts = parts;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal servicesTotal = BigDecimal.ZERO;
        if (services != null) {
            servicesTotal = services.stream()
                    .filter(s -> s.getFinalPrice() != null)
                    .map(s -> s.getFinalPrice().multiply(BigDecimal.valueOf(s.getQuantity() != null ? s.getQuantity() : 1)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal partsTotal = BigDecimal.ZERO;
        if (parts != null) {
            partsTotal = parts.stream()
                    .filter(p -> p.getFinalPrice() != null)
                    .map(p -> p.getFinalPrice().multiply(BigDecimal.valueOf(p.getQuantity() != null ? p.getQuantity() : 1)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return servicesTotal.add(partsTotal);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceItemDto {
        private String serviceName;
        private Integer quantity;
        private BigDecimal finalPrice;
        private String mechanicName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartItemDto {
        private String partName;
        private String oemNumber;
        private Integer quantity;
        private BigDecimal finalPrice;
    }
}
