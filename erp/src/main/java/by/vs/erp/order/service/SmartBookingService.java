package by.vs.erp.order.service;

import by.vs.erp.order.dto.TimeSlotDto;
import by.vs.erp.order.entity.Booking;
import by.vs.erp.order.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SmartBookingService {

    private final BookingRepository bookingRepository;

    public List<TimeSlotDto> findAvailableSlots(LocalDateTime day, int durationHours) {
        LocalDateTime workStart = day.withHour(9).withMinute(0).withSecond(0); // СТО открывается в 09:00
        LocalDateTime workEnd = day.withHour(21).withMinute(0).withSecond(0);   // СТО закрывается в 21:00

        List<Booking> dayBookings = bookingRepository.findOverlappingBookings(workStart, workEnd);

        List<TimeSlotDto> freeSlots = new ArrayList<>();
        LocalDateTime currentPointer = workStart;

        for (Booking booking : dayBookings) {
            if (currentPointer.plusHours(durationHours).isBefore(booking.getStartTime()) ||
                    currentPointer.plusHours(durationHours).isEqual(booking.getStartTime())) {
                freeSlots.add(new TimeSlotDto(currentPointer, booking.getStartTime()));
            }
            if (booking.getEndTime().isAfter(currentPointer)) {
                currentPointer = booking.getEndTime();
            }
        }

        if (currentPointer.plusHours(durationHours).isBefore(workEnd)) {
            freeSlots.add(new TimeSlotDto(currentPointer, workEnd));
        }

        return freeSlots;
    }
}

