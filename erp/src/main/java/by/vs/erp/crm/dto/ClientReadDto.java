package by.vs.erp.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientReadDto {
    private Long id;
    private String phone;
    private String lastName;
    private String firstName;
}
