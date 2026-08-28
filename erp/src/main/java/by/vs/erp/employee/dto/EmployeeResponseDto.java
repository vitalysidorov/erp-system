package by.vs.erp.employee.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class EmployeeResponseDto {
    private Long id;
    private String email;
    private String lastName;
    private String firstName;
    private String role;
    private BigDecimal salaryRatePercent;
    private Boolean isActive;
}