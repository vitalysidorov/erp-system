package by.vs.bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email не должен быть пустым")
    @Email(message = "Введен некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль не должен быть пустым")
    private String password;
}
