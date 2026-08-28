package by.vs.bff.dto;

import java.time.LocalDateTime;

public record ClientBookingResponse(
        Long bookingId,
        Long vehicleId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {}
