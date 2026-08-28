package by.vs.erp.order.controller;

import by.vs.erp.order.dto.CreateWorkOrderRequestDto;
import by.vs.erp.order.dto.WorkOrderResponseDto;
import by.vs.erp.order.entity.ServiceCatalog;
import by.vs.erp.order.mapper.WorkOrderMapper;
import by.vs.erp.order.repository.WorkOrderRepository;
import by.vs.erp.order.service.ServiceCatalogService;
import by.vs.erp.order.service.WorkOrderService;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
@Slf4j
public class WorkOrderRestController {

    private final WorkOrderService workOrderService;
    private final WorkOrderRepository workOrderRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final WorkOrderMapper workOrderMapper;

    @PostMapping
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponseDto> openWorkOrder(@Valid @RequestBody CreateWorkOrderRequestDto dto) {
        log.info("API: Запрос на открытие заказ-наряда для авто с VIN: {}", dto.getVin());
        WorkOrderResponseDto response = workOrderService.createWorkOrder(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{orderId}/services")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Void> addServiceToOrder(
            @PathVariable Long orderId,
            @RequestParam Long serviceCatalogId,
            @RequestParam Long mechanicId) {
        log.info("API: Добавление услуги #{} в заказ-наряд #{} для механика #{}", serviceCatalogId, orderId, mechanicId);

        ServiceCatalog service = serviceCatalogService.findById(serviceCatalogId);

        Employee mechanic = employeeRepository.findById(mechanicId)
                .orElseThrow(() -> new IllegalArgumentException("Указанный механик не найден в штате"));

        workOrderService.addServiceToOrder(orderId, service, mechanic);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam String newStatus,
            @AuthenticationPrincipal String managerEmail) {

        log.info("API: Изменение статуса заказа #{} на '{}' оператором с email: {}", id, newStatus, managerEmail);

        Employee manager = employeeRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Авторизованный менеджер/мастер не найден в системе"));

        workOrderService.changeOrderStatus(id, newStatus, manager);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponseDto> closeOrder(@PathVariable Long id) {
        log.info("API: Запуск финального закрытия и кассового расчета для заказа #{}", id);
        WorkOrderResponseDto closedOrder = workOrderService.closeWorkOrder(id);
        return ResponseEntity.ok(closedOrder);
    }

    @GetMapping("/{id}")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponseDto> getOrderDetails(@PathVariable Long id) {
        return workOrderRepository.findByIdWithDetails(id)
                .map(workOrderMapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDto>> getActiveOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<WorkOrderResponseDto> activeOrders = workOrderRepository.findByStatus(
                        "IN_PROGRESS",
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(workOrderMapper::toResponseDto);
        return ResponseEntity.ok(activeOrders);
    }

    @GetMapping("/closed")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDto>> getClosedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<WorkOrderResponseDto> closedOrders = workOrderRepository.findByStatus(
                        "CLOSED",
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(workOrderMapper::toResponseDto);
        return ResponseEntity.ok(closedOrders);
    }

    @GetMapping("/opened")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDto>> getOpenedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<WorkOrderResponseDto> openedOrders = workOrderRepository.findByStatus(
                        "OPENED",
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(workOrderMapper::toResponseDto);
        return ResponseEntity.ok(openedOrders);
    }

    @GetMapping("/completed")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDto>> getCompletedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<WorkOrderResponseDto> completedOrders = workOrderRepository.findByStatus(
                        "COMPLETED",
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(workOrderMapper::toResponseDto);
        return ResponseEntity.ok(completedOrders);
    }

    @GetMapping("/vin")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Slice<WorkOrderResponseDto>> getOrdersByVin(@RequestParam String vin,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "15") int size) {
        Slice<WorkOrderResponseDto> orders = workOrderRepository.findByVin(vin,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(workOrderMapper::toResponseDto);
        return ResponseEntity.ok(orders);
    }
}


