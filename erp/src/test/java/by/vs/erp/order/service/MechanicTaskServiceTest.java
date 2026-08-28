package by.vs.erp.order.service;

import by.vs.erp.order.dto.MechanicTaskDto;
import by.vs.erp.order.entity.OrderServiceItem;
import by.vs.erp.order.mapper.MechanicTaskMapper;
import by.vs.erp.order.repository.OrderServiceItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MechanicTaskServiceTest {

    @Mock
    private OrderServiceItemRepository serviceItemRepository;

    @Mock
    private MechanicTaskMapper mechanicTaskMapper;

    @InjectMocks
    private MechanicTaskService mechanicTaskService;

    @Test
    @DisplayName("getActiveTasksForMechanic: Должен вернуть список отмаппленных DTO задач для механика")
    void shouldGetActiveTasksForMechanic() {
        Long mechanicId = 5L;
        OrderServiceItem item1 = new OrderServiceItem();
        OrderServiceItem item2 = new OrderServiceItem();

        MechanicTaskDto dto1 = new MechanicTaskDto(10L, 100L, "Audi A6", "Замена колодок", new BigDecimal("0.80"));
        MechanicTaskDto dto2 = new MechanicTaskDto(10L, 101L, "Audi A6", "Диагностика подвески", new BigDecimal("0.50"));

        when(serviceItemRepository.findActiveTasksByMechanic(mechanicId))
                .thenReturn(List.of(item1, item2));
        when(mechanicTaskMapper.toTaskDto(item1)).thenReturn(dto1);
        when(mechanicTaskMapper.toTaskDto(item2)).thenReturn(dto2);

        List<MechanicTaskDto> activeTasks = mechanicTaskService.getActiveTasksForMechanic(mechanicId);

        assertNotNull(activeTasks);
        assertEquals(2, activeTasks.size());
        assertEquals("Замена колодок", activeTasks.get(0).getServiceName());
        assertEquals(new BigDecimal("0.50"), activeTasks.get(1).getNormHours());
        verify(serviceItemRepository, times(1)).findActiveTasksByMechanic(mechanicId);
    }
}