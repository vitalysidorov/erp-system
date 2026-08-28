package by.vs.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlotDto {
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
}

