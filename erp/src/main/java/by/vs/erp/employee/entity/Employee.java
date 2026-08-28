package by.vs.erp.employee.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "usr_employees")
@Getter
@Setter
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Email обязателен для заполнения")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String password;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 30)
    private String role; // MASTER, MECHANIC, MANAGER

    @Column(name = "salary_rate_percent", precision = 5, scale = 2)
    private BigDecimal salaryRatePercent;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
