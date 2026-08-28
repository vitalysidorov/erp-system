package by.vs.bff.dto;

import java.time.LocalDateTime;

public record ClientBookingRequest(
        Long vehicleId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}
