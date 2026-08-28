package by.vs.erp.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientDto {
    @NotBlank(message = "Номер телефона обязателен")
    private String phone;

    @NotBlank(message = "Фамилия клиента обязательна")
    private String lastName;

    @NotBlank(message = "Имя клиента обязательно")
    private String firstName;
}
