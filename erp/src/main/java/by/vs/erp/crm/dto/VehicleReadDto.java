package by.vs.erp.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VehicleReadDto {
    private Long id;
    private String vin;
    private String clientId;
    private String make;
    private String model;
}