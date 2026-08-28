package by.vs.erp.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class EmployeeRegisterRequest {

    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Email обязателен для заполнения")
    @Size(max = 100, message = "Email не должен превышать 100 символов")
    private String email;

    @NotBlank(message = "Пароль обязателен для заполнения")
    @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
    private String password;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
    private String lastName;

    @NotBlank(message = "Имя обязательно")
    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    private String firstName;

    @NotBlank(message = "Роль обязательна")
    @Pattern(regexp = "MASTER|MECHANIC|MANAGER", message = "Роль должна быть MASTER, MECHANIC или MANAGER")
    private String role;

    private BigDecimal salaryRatePercent;
}
