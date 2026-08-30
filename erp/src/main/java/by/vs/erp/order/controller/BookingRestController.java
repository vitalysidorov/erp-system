package by.vs.erp.order.controller;

import by.vs.erp.order.dto.BookingRequestDto;
import by.vs.erp.order.dto.BookingResponseDto;
import by.vs.erp.order.dto.TimeSlotDto;
import by.vs.erp.order.service.BookingService;
import by.vs.erp.order.service.SmartBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingRestController {

    private final BookingService bookingService;
    private final SmartBookingService smartBookingService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER') or (hasRole('CLIENT') and @bookingSecurityService.isVehicleOwner(#dto.vehicleId, authentication))")
    public ResponseEntity<BookingResponseDto> createBooking(@Valid @RequestBody BookingRequestDto dto) {
        BookingResponseDto created = bookingService.createBooking(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER') or (hasRole('CLIENT') and @bookingSecurityService.isBookingOwner(#id, authentication))")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/suggest-slots")
    @PreAuthorize("hasRole('MANAGER') or (hasRole('CLIENT'))")
    public ResponseEntity<List<TimeSlotDto>> getFreeSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestParam int durationHours) {
        return ResponseEntity.ok(smartBookingService.findAvailableSlots(date, durationHours));
    }
}