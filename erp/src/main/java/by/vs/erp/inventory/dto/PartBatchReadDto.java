package by.vs.erp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PartBatchReadDto {
    private Long id;
    private Long partId;
    private Integer initialQuantity;
    private Integer availableQuantity;
    private BigDecimal purchasePrice;
    private LocalDateTime receivedAt;
}
