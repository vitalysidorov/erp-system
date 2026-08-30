package by.vs.erp.order.service;

import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.crm.repository.VehicleRepository;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import by.vs.erp.inventory.entity.PartBatch;
import by.vs.erp.inventory.entity.PartCatalog;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.PartBatchRepository;
import by.vs.erp.inventory.repository.StockRepository;
import by.vs.erp.order.dto.CreateWorkOrderRequestDto;
import by.vs.erp.order.dto.WorkOrderResponseDto;
import by.vs.erp.order.entity.*;
import by.vs.erp.order.event.ClosedWorkOrderEvent;
import by.vs.erp.order.mapper.WorkOrderMapper;
import by.vs.erp.order.repository.CarClassRepository;
import by.vs.erp.order.repository.WorkOrderRepository;
import by.vs.erp.order.repository.WorkOrderStatusLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private WorkOrderStatusLogRepository statusLogRepository;
    @Mock private StockRepository stockRepository;
    @Mock private WorkOrderMapper workOrderMapper;
    @Mock private CarClassRepository carClassRepository;
    @Mock private PartBatchRepository partBatchRepository;
    @Mock private RabbitTemplate rabbitTemplate; // новая зависимость: уведомление о дефиците склада

    @InjectMocks
    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workOrderService, "minLimit", 5);
    }

    @Test
    @DisplayName("createWorkOrder: Успешное открытие заказа для существующих клиента и авто с логированием")
    void shouldCreateWorkOrderSuccessfully() {
        CreateWorkOrderRequestDto dto = new CreateWorkOrderRequestDto(
                "+375291112233", "Иван", "Петров",
                "VIN123456789ABCDEF", "Audi", "A6", "1111-PM-7",
                10L, 150000, "1/2", "Царапина"
        );

        Client existingClient = new Client();
        existingClient.setId(1L);
        existingClient.setPhone(dto.getClientPhone());

        Vehicle existingVehicle = new Vehicle();
        existingVehicle.setId(2L);
        existingVehicle.setVin(dto.getVin());
        existingVehicle.setClient(existingClient);

        Employee master = new Employee();
        master.setId(10L);

        WorkOrder mockSavedOrder = new WorkOrder();
        mockSavedOrder.setId(500L);

        when(clientRepository.findByPhone(dto.getClientPhone())).thenReturn(Optional.of(existingClient));
        when(vehicleRepository.findByVin(dto.getVin())).thenReturn(Optional.of(existingVehicle));
        when(employeeRepository.findById(dto.getMasterId())).thenReturn(Optional.of(master));
        when(workOrderRepository.save(any(WorkOrder.class))).thenReturn(mockSavedOrder);
        when(workOrderMapper.toResponseDto(any(WorkOrder.class))).thenReturn(new WorkOrderResponseDto());

        WorkOrderResponseDto response = workOrderService.createWorkOrder(dto);

        assertNotNull(response);
        verify(workOrderRepository, times(1)).save(any(WorkOrder.class));

        ArgumentCaptor<WorkOrderStatusLog> logCaptor = ArgumentCaptor.forClass(WorkOrderStatusLog.class);
        verify(statusLogRepository, times(1)).save(logCaptor.capture());

        WorkOrderStatusLog savedLog = logCaptor.getValue();
        assertNull(savedLog.getFromStatus());
        assertEquals("OPENED", savedLog.getToStatus());
        assertEquals(master, savedLog.getChangedBy());

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("closeWorkOrder: Успешное закрытие ремонта, списание остатков склада и публикация финансового события")
    void shouldCloseWorkOrderSuccessfully() {
        Long orderId = 100L;

        PartCatalog part = new PartCatalog();
        part.setId(10L);
        part.setName("Колодки тормозные");

        OrderPartItem partItem = new OrderPartItem();
        partItem.setPart(part);
        partItem.setQuantity(2);

        WorkOrder order = new WorkOrder();
        order.setId(orderId);
        order.setStatus("IN_PROGRESS");
        order.setParts(List.of(partItem));

        Stock stock = new Stock();
        stock.setPart(part);
        stock.setQuantity(10);

        WorkOrderResponseDto responseDto = new WorkOrderResponseDto();
        responseDto.setTotalAmount(new BigDecimal("250.00"));

        when(workOrderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(stockRepository.findByPartId(10L)).thenReturn(Optional.of(stock));
        when(workOrderMapper.toResponseDto(order)).thenReturn(responseDto);

        WorkOrderResponseDto result = workOrderService.closeWorkOrder(orderId);

        assertNotNull(result);
        assertEquals("CLOSED", order.getStatus());
        assertNotNull(order.getClosedAt());
        // 10 - 2 = 8, что выше minLimit(5) → уведомление о дефиците НЕ должно уйти
        assertEquals(8, stock.getQuantity());

        verify(stockRepository, times(1)).save(stock);
        verify(workOrderRepository, times(1)).save(order);
        verify(eventPublisher, times(1)).publishEvent(any(ClosedWorkOrderEvent.class));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("closeWorkOrder: При падении остатка ниже минимального лимита публикуется событие дефицита в RabbitMQ")
    void shouldPublishDeficiencyEventWhenStockFallsBelowMinLimit() {
        Long orderId = 200L;

        PartCatalog part = new PartCatalog();
        part.setId(20L);
        part.setName("Фильтр масляный");
        part.setOemNumber("W71294");

        OrderPartItem partItem = new OrderPartItem();
        partItem.setPart(part);
        partItem.setQuantity(6);

        WorkOrder order = new WorkOrder();
        order.setId(orderId);
        order.setStatus("IN_PROGRESS");
        order.setParts(List.of(partItem));

        Stock stock = new Stock();
        stock.setPart(part);
        stock.setQuantity(8); // 8 - 6 = 2, что <= minLimit(5)

        WorkOrderResponseDto responseDto = new WorkOrderResponseDto();
        responseDto.setTotalAmount(BigDecimal.TEN);

        when(workOrderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(stockRepository.findByPartId(20L)).thenReturn(Optional.of(stock));
        when(workOrderMapper.toResponseDto(order)).thenReturn(responseDto);

        workOrderService.closeWorkOrder(orderId);

        assertEquals(2, stock.getQuantity());
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("erp.inventory.exchange"),
                eq("stock.deficiency"),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("closeWorkOrder: Выброс IllegalStateException, если на складе недостаточно запчастей")
    void shouldThrowExceptionWhenStockIsInsufficient() {
        Long orderId = 100L;

        PartCatalog part = new PartCatalog();
        part.setId(10L);
        part.setName("Свеча зажигания");

        OrderPartItem partItem = new OrderPartItem();
        partItem.setPart(part);
        partItem.setQuantity(4);

        WorkOrder order = new WorkOrder();
        order.setId(orderId);
        order.setStatus("IN_PROGRESS");
        order.setParts(List.of(partItem));

        Stock stock = new Stock();
        stock.setPart(part);
        stock.setQuantity(2);

        when(workOrderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(stockRepository.findByPartId(10L)).thenReturn(Optional.of(stock));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                workOrderService.closeWorkOrder(orderId)
        );

        assertTrue(exception.getMessage().contains("Недостаточно деталей на складе"));
        assertEquals("IN_PROGRESS", order.getStatus());
        verify(workOrderRepository, never()).save(order);
        verify(eventPublisher, never()).publishEvent(any());
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("addServiceToOrder: Успешное добавление услуги с расчётом цены с коэффициентом класса авто")
    void shouldAddServiceToOrderWithCarClassCoefficient() {
        Long orderId = 1L;

        Vehicle vehicle = new Vehicle();
        vehicle.setMake("Porsche");

        WorkOrder order = new WorkOrder();
        order.setId(orderId);
        order.setVehicle(vehicle);
        order.setServices(new ArrayList<>());

        ServiceCatalog service = new ServiceCatalog();
        service.setId(50L);
        service.setHourRatePrice(new BigDecimal("100.00"));
        service.setNormHours(new BigDecimal("2.5"));

        Employee mechanic = new Employee();
        mechanic.setId(30L);

        CarClass carClass = new CarClass();
        carClass.setBrand("Porsche");
        carClass.setPriceCoefficient(new BigDecimal("1.50"));

        when(workOrderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(carClassRepository.findByBrand("Porsche")).thenReturn(Optional.of(carClass));

        workOrderService.addServiceToOrder(orderId, service, mechanic);

        verify(workOrderRepository, times(1)).save(order);
        assertEquals(1, order.getServices().size());

        BigDecimal expectedPrice = new BigDecimal("375.0000");
        assertEquals(0, expectedPrice.compareTo(order.getServices().get(0).getFinalPrice()));
    }

    @Test
    @DisplayName("allocateParts: Успешное FIFO списание потребности из нескольких последовательных партий")
    void shouldAllocatePartsUsingFifoStrategy() {
        PartCatalog part = new PartCatalog();
        part.setId(99L);
        part.setName("Свеча зажигания");

        OrderPartItem partItem = new OrderPartItem();
        partItem.setPart(part);
        partItem.setQuantity(5);

        WorkOrder order = new WorkOrder();
        order.setParts(List.of(partItem));

        PartBatch oldBatch = new PartBatch();
        oldBatch.setId(1L);
        oldBatch.setAvailableQuantity(2);

        PartBatch newBatch = new PartBatch();
        newBatch.setId(2L);
        newBatch.setAvailableQuantity(10);

        when(partBatchRepository.findAvailableBatches(99L)).thenReturn(List.of(oldBatch, newBatch));

        workOrderService.allocateParts(order);

        assertEquals(0, oldBatch.getAvailableQuantity());
        assertEquals(7, newBatch.getAvailableQuantity());

        verify(partBatchRepository, times(1)).save(oldBatch);
        verify(partBatchRepository, times(1)).save(newBatch);
    }

    @Test
    @DisplayName("allocateParts: Выброс IllegalStateException, если суммарный остаток всех партий меньше требуемого")
    void shouldThrowExceptionWhenTotalBatchesQuantityIsInsufficient() {
        PartCatalog part = new PartCatalog();
        part.setId(99L);
        part.setName("Масло моторное");

        OrderPartItem partItem = new OrderPartItem();
        partItem.setPart(part);
        partItem.setQuantity(6);

        WorkOrder order = new WorkOrder();
        order.setParts(List.of(partItem));

        PartBatch batch = new PartBatch();
        batch.setAvailableQuantity(4);

        when(partBatchRepository.findAvailableBatches(99L)).thenReturn(List.of(batch));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                workOrderService.allocateParts(order)
        );

        assertTrue(exception.getMessage().contains("Недостаточно остатков для детали"));
        verify(partBatchRepository, never()).save(any());
    }
}