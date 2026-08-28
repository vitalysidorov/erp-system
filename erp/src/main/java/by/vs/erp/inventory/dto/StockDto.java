package by.vs.erp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class StockDto {
    private Long id;

    @NotNull(message = "Укажите деталь")
    private Long partId;

    @NotNull(message = "Укажите количество")
    @PositiveOrZero(message = "Количество не может быть отрицательным")
    private Integer quantity;

    @NotNull(message = "Укажите цену закупки")
    private BigDecimal purchasePrice;

    @NotNull(message = "Укажите цену продажи")
    private BigDecimal retailPrice;
}
