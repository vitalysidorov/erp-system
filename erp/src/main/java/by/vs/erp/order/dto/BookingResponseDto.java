package by.vs.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class BookingResponseDto {
    private Long id;
    private String createdBy;
    private String source;
    private Long vehicleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
