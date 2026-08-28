package by.vs.erp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateWorkOrderRequestDto {
    @NotBlank(message = "Номер телефона обязателен")
    private String clientPhone;
    @NotBlank(message = "Имя клиента обязательно")
    private String clientFirstName;
    @NotBlank(message = "Фамилия клиента обязательна")
    private String clientLastName;

    @NotBlank(message = "VIN обязателен")
    @Size(min = 17, max = 17, message = "VIN должен состоять строго из 17 символов")
    private String vin;
    @NotBlank(message = "Марка обязательна")
    private String make;
    @NotBlank(message = "Модель обязательна")
    private String model;
    private String plateNumber;

    @NotNull(message = "ID мастера-приемщика обязателен")
    private Long masterId;
    @NotNull(message = "Пробег обязателен")
    private Integer mileageIn;
    private String fuelLevel;
    private String damagesNotes;
}

