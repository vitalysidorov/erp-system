package by.vs.erp.order.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.order.dto.BookingRequestDto;
import by.vs.erp.order.dto.BookingResponseDto;
import by.vs.erp.order.dto.TimeSlotDto;
import by.vs.erp.order.entity.Booking;
import by.vs.erp.order.repository.BookingRepository;
import by.vs.erp.order.service.BookingService;
import by.vs.erp.order.service.SmartBookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private SmartBookingService smartBookingService;

    @MockBean(name = "bookingSecurityService")
    private Object bookingSecurityService;

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /bookings: Успешное создание бронирования менеджером с возвратом BookingResponseDto")
    void shouldCreateBookingWhenUserIsManager() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime end = start.plusHours(2);
        BookingRequestDto dto = new BookingRequestDto(1L, start, end, "manager", "MANUAL");

        BookingResponseDto responseDto = BookingResponseDto.builder()
                .id(55L)
                .createdBy("manager")
                .source("MANUAL")
                .vehicleId(1L)
                .startTime(start)
                .endTime(end)
                .status("CONFIRMED")
                .build();

        Mockito.when(bookingService.createBooking(any(BookingRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55L))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.createdBy").value("manager"))
                .andExpect(jsonPath("$.vehicleId").value(1L));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("POST /bookings: Отказ в доступе (403) для роли MASTER")
    void shouldReturnForbiddenWhenUserIsMaster() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1), "client", "BFF_WEB");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /suggest-slots: Успешное получение списка свободных окон для записи")
    void shouldSuggestFreeSlots() throws Exception {
        LocalDateTime targetDate = LocalDateTime.of(2026, 9, 1, 10, 0);
        TimeSlotDto slot = new TimeSlotDto(targetDate, targetDate.plusHours(2));

        Mockito.when(smartBookingService.findAvailableSlots(eq(targetDate), eq(2)))
                .thenReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/bookings/suggest-slots")
                        .param("date", "2026-09-01T10:00:00")
                        .param("durationHours", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotStart").exists())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("PATCH /id/cancel: Успешная отмена бронирования")
    void shouldCancelBookingSuccessfully() throws Exception {
        Long bookingId = 10L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus("PENDING");

        Mockito.when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        mockMvc.perform(patch("/api/v1/bookings/{id}/cancel", bookingId))
                .andExpect(status().isNoContent());

        Mockito.verify(bookingRepository, Mockito.times(1)).save(booking);
        org.junit.jupiter.api.Assertions.assertEquals("CANCELLED", booking.getStatus());
    }
}
