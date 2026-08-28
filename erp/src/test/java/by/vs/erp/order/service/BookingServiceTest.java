package by.vs.erp.order.service;

import by.vs.erp.common.security.UserPrincipal;
import by.vs.erp.order.dto.BookingRequestDto;
import by.vs.erp.order.dto.BookingResponseDto;
import by.vs.erp.order.entity.Booking;
import by.vs.erp.order.mapper.BookingMapper;
import by.vs.erp.order.repository.BookingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createBooking: Успешное бронирование с автоподстановкой текущего пользователя и возвратом BookingResponseDto")
    void shouldCreateBookingSuccessfully() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        BookingRequestDto dto = new BookingRequestDto(1L, start, end, null, "MANUAL");

        Booking bookingEntity = new Booking();
        bookingEntity.setStartTime(start);
        bookingEntity.setEndTime(end);

        BookingResponseDto expectedResponse = BookingResponseDto.builder()
                .id(77L)
                .createdBy("USR-777")
                .source("MANUAL")
                .vehicleId(1L)
                .startTime(start)
                .endTime(end)
                .status("CONFIRMED")
                .build();

        when(bookingRepository.findOverlappingBookingsWithLock(1L, start, end))
                .thenReturn(List.of());
        when(bookingMapper.toEntity(dto)).thenReturn(bookingEntity);
        when(bookingRepository.save(bookingEntity)).thenReturn(bookingEntity);
        when(bookingMapper.toDto(bookingEntity)).thenReturn(expectedResponse);

        UserPrincipal mockPrincipal = mock(UserPrincipal.class);
        when(mockPrincipal.id()).thenReturn("USR-777");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(mockPrincipal);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        BookingResponseDto result = bookingService.createBooking(dto);

        assertNotNull(result);
        assertEquals(77L, result.getId());
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("USR-777", result.getCreatedBy());
        assertEquals(1L, result.getVehicleId());

        verify(bookingRepository, times(1)).save(bookingEntity);
    }


    @Test
    @DisplayName("createBooking: Выброс IllegalStateException при обнаружении пересечения времени (Overlapping)")
    void shouldThrowExceptionWhenBookingOverlaps() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);
        BookingRequestDto dto = new BookingRequestDto(1L, start, end, "operator", "MANUAL");

        Booking existingBooking = new Booking();
        when(bookingRepository.findOverlappingBookingsWithLock(1L, start, end))
                .thenReturn(List.of(existingBooking));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bookingService.createBooking(dto)
        );

        assertEquals("Выбранный временной интервал уже занят для данного транспорта или поста.", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(bookingMapper);
    }
}