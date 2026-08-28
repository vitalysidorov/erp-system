package by.vs.erp.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VehicleDto {

    @NotBlank(message = "VIN обязателен")
    @Size(min = 17, max = 17, message = "VIN должен состоять строго из 17 символов")
    private String vin;

    @NotNull(message = "Владелец обязателен")
    private Long clientId;

    @NotBlank(message = "Марка обязательна")
    private String make;

    @NotBlank(message = "Модель обязательна")
    private String model;

    private String plateNumber;
}
