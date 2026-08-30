package by.vs.erp.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ClientDto {
    @NotBlank(message = "Номер телефона обязателен")
    private String phone;

    @NotBlank(message = "Пароль обязателен")
    private String password;

    @NotBlank(message = "Фамилия клиента обязательна")
    private String lastName;

    @NotBlank(message = "Имя клиента обязательно")
    private String firstName;
}
