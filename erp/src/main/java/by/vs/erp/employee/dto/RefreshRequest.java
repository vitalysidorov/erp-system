package by.vs.erp.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh токен не может быть пустым")
    private String refreshToken;
}
