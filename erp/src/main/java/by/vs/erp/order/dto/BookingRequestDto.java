package by.vs.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequestDto {
    private Long vehicleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String createdBy;
    private String source;
}


