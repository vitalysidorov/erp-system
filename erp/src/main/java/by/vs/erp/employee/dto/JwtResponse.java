package by.vs.erp.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtResponse {
    private final String type = "Bearer";
    private final String accessToken;
    private final String refreshToken;
}
