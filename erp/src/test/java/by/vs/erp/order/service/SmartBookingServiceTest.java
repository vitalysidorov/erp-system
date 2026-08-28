package by.vs.erp.order.service;

import by.vs.erp.order.dto.TimeSlotDto;
import by.vs.erp.order.entity.Booking;
import by.vs.erp.order.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private SmartBookingService smartBookingService;

    @Test
    @DisplayName("findAvailableSlots: Должен вернуть полный рабочий день, если бронирования отсутствуют")
    void shouldReturnFullDayWhenNoBookingsExist() {
        LocalDateTime targetDay = LocalDateTime.of(2026, 9, 1, 12, 0); // 1 Сентября 2026
        int durationHours = 2;

        when(bookingRepository.findOverlappingBookings(any(), any())).thenReturn(List.of());

        List<TimeSlotDto> availableSlots = smartBookingService.findAvailableSlots(targetDay, durationHours);

        assertNotNull(availableSlots);
        assertEquals(1, availableSlots.size(), "Должен вернуться один общий свободный слот");

        TimeSlotDto fullDaySlot = availableSlots.get(0);
        assertEquals(targetDay.withHour(9).withMinute(0).withSecond(0), fullDaySlot.getSlotStart(), "Начало в 09:00");
        assertEquals(targetDay.withHour(21).withMinute(0).withSecond(0), fullDaySlot.getSlotEnd(), "Конец в 21:00");
    }

    @Test
    @DisplayName("findAvailableSlots: Должен найти свободные окна до и после существующей брони посередине дня")
    void shouldFindSlotsAroundExistingBooking() {
        LocalDateTime targetDay = LocalDateTime.of(2026, 9, 1, 0, 0);
        int durationHours = 2;

        Booking booking = new Booking();
        booking.setStartTime(targetDay.withHour(13).withMinute(0).withSecond(0));
        booking.setEndTime(targetDay.withHour(15).withMinute(0).withSecond(0));

        when(bookingRepository.findOverlappingBookings(any(), any())).thenReturn(List.of(booking));

        List<TimeSlotDto> availableSlots = smartBookingService.findAvailableSlots(targetDay, durationHours);

        assertNotNull(availableSlots);
        assertEquals(2, availableSlots.size(), "Должно образоваться два свободных окна");

        TimeSlotDto firstSlot = availableSlots.get(0);
        assertEquals(targetDay.withHour(9).withMinute(0), firstSlot.getSlotStart());
        assertEquals(targetDay.withHour(13).withMinute(0), firstSlot.getSlotEnd());

        TimeSlotDto secondSlot = availableSlots.get(1);
        assertEquals(targetDay.withHour(15).withMinute(0), secondSlot.getSlotStart());
        assertEquals(targetDay.withHour(21).withMinute(0), secondSlot.getSlotEnd());
    }

    @Test
    @DisplayName("findAvailableSlots: Должен вернуть пустой список, если свободные окна меньше запрашиваемой длительности")
    void shouldReturnEmptyListWhenNoSlotsAreLongEnough() {
        LocalDateTime targetDay = LocalDateTime.of(2026, 9, 1, 0, 0);
        int durationHours = 4;

        Booking booking1 = new Booking();
        booking1.setStartTime(targetDay.withHour(11).withMinute(0));
        booking1.setEndTime(targetDay.withHour(13).withMinute(0));

        Booking booking2 = new Booking();
        booking2.setStartTime(targetDay.withHour(15).withMinute(0));
        booking2.setEndTime(targetDay.withHour(18).withMinute(0));

        when(bookingRepository.findOverlappingBookings(any(), any())).thenReturn(List.of(booking1, booking2));

        List<TimeSlotDto> availableSlots = smartBookingService.findAvailableSlots(targetDay, durationHours);

        assertNotNull(availableSlots);
        assertTrue(availableSlots.isEmpty(), "Свободных слотов подходящей длины быть не должно");
    }
}
