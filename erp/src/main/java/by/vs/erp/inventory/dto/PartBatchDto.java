package by.vs.erp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PartBatchDto {

    @NotNull(message = "Укажите деталь")
    private Long partId;

    @NotNull(message = "Укажите начальное количество")
    @Positive(message = "Количество должно быть больше нуля")
    private Integer initialQuantity;

    @NotNull(message = "Укажите доступное количество")
    private Integer availableQuantity;

    @NotNull(message = "Укажите цену закупки")
    private BigDecimal purchasePrice;
}
