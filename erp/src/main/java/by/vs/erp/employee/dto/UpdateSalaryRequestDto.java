package by.vs.erp.employee.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class UpdateSalaryRequestDto {
    @NotNull(message = "Ставка зарплаты обязательна")
    @DecimalMin(value = "0.00", message = "Ставка не может быть отрицательной")
    @DecimalMax(value = "100.00", message = "Ставка не может превышать 100%")
    private BigDecimal newSalaryRatePercent;
}