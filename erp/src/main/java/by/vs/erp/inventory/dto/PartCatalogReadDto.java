package by.vs.erp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PartCatalogReadDto {
    private Long id;
    private String oemNumber;
    private String name;
    private String brand;
}
