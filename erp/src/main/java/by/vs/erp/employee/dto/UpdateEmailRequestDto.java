package by.vs.erp.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmailRequestDto {
    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Новый email обязателен")
    private String newEmail;
}