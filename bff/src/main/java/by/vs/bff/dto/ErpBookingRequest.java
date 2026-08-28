package by.vs.bff.dto;

import java.time.LocalDateTime;

public record ErpBookingRequest(
        Long vehicleId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String createdBy,
        String source
) {}
