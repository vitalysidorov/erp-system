package by.vs.erp.order.service;

import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.crm.repository.VehicleRepository;
import by.vs.erp.inventory.entity.PartBatch;
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
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {

    private final ApplicationEventPublisher eventPublisher;
    private final WorkOrderRepository workOrderRepository;
    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkOrderStatusLogRepository statusLogRepository;
    private final WorkOrderMapper workOrderMapper;
    private final StockRepository stockRepository;
    private final CarClassRepository carClassRepository;
    private final PartBatchRepository partBatchRepository;

    @Value("${spring.application.inventory.min-limit}")
    private Integer minLimit;

    @Transactional
    public WorkOrderResponseDto closeWorkOrder(Long orderId) {
        log.info("Запуск процедуры закрытия заказ-наряда №{}", orderId);

        WorkOrder order = workOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ-наряд не найден"));

        if ("CLOSED".equals(order.getStatus())) {
            throw new IllegalStateException("Этот заказ-наряд уже закрыт.");
        }

        order.getParts().forEach(item -> {
            Stock stock = stockRepository.findByPartId(item.getPart().getId())
                    .orElseThrow(() -> new IllegalStateException("Деталь " + item.getPart().getName() + " отсутствует на складе"));

            if (stock.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Недостаточно деталей на складе: " + item.getPart().getName());
            }

            // уменьшаем остаток на складе
            int newQuantity = stock.getQuantity() - item.getQuantity();
            stock.setQuantity(newQuantity);
            stockRepository.save(stock);

            // проверка на падение ниже минимального лимита
            if (newQuantity <= minLimit) {
                log.warn("Деталь '{}' (OEM: {}) требует автозаказа. Осталось: {}",
                        item.getPart().getName(), item.getPart().getOemNumber(), newQuantity);
            }
        });

        // переводим статус документа в финальный
        order.setStatus("CLOSED");
        order.setClosedAt(LocalDateTime.now());
        workOrderRepository.save(order);

        // расчет полной стоимости
        WorkOrderResponseDto responseDto = workOrderMapper.toResponseDto(order);
        BigDecimal finalRevenue = responseDto.getTotalAmount();

        // оформление первичного дохода в кассу
        eventPublisher.publishEvent(new ClosedWorkOrderEvent(order, finalRevenue));

        log.info("Заказ-наряд №{} успешно закрыт. Проведена транзакция на сумму: {} руб.", orderId, finalRevenue);
        return responseDto;
    }

    @Transactional
    public WorkOrderResponseDto createWorkOrder(CreateWorkOrderRequestDto dto) {
        log.info("Старт создания заказ-наряда для автомобиля с VIN: {}", dto.getVin());

        Client client = clientRepository.findByPhone(dto.getClientPhone())
                .orElseGet(() -> {
                    log.info("Клиент с телефоном {} не найден. Создаем новый профиль.", dto.getClientPhone());
                    Client newClient = new Client();
                    newClient.setPhone(dto.getClientPhone());
                    newClient.setFirstName(dto.getClientFirstName());
                    newClient.setLastName(dto.getClientLastName());
                    return clientRepository.save(newClient);
                });

        Vehicle vehicle = vehicleRepository.findByVin(dto.getVin())
                .orElseGet(() -> {
                    log.info("Автомобиль с VIN {} оформляется впервые. Привязываем к клиенту ID: {}", dto.getVin(), client.getId());
                    Vehicle newVehicle = new Vehicle();
                    newVehicle.setVin(dto.getVin());
                    newVehicle.setMake(dto.getMake());
                    newVehicle.setModel(dto.getModel());
                    newVehicle.setPlateNumber(dto.getPlateNumber());
                    newVehicle.setClient(client);
                    return vehicleRepository.save(newVehicle);
                });

        if (!vehicle.getClient().getId().equals(client.getId())) {
            log.warn("Внимание: Автомобиль {} сменил владельца. Перепривязка на клиента ID: {}", vehicle.getVin(), client.getId());
            vehicle.setClient(client);
            vehicleRepository.save(vehicle);
        }

        Employee master = employeeRepository.findById(dto.getMasterId())
                .orElseThrow(() -> new IllegalArgumentException("Мастер-приемщик с ID " + dto.getMasterId() + " не найден"));

        // инициализация и сохранение самого заказ-наряда
        WorkOrder order = new WorkOrder();
        order.setVehicle(vehicle);
        order.setMaster(master);
        order.setStatus("OPENED");
        order.setMileageIn(dto.getMileageIn());
        order.setFuelLevel(dto.getFuelLevel());
        order.setDamagesNotes(dto.getDamagesNotes());

        WorkOrder savedOrder = workOrderRepository.save(order);

        // логирование создания документа в таблицу аудита
        WorkOrderStatusLog initialLog = new WorkOrderStatusLog();
        initialLog.setWorkOrder(savedOrder);
        initialLog.setFromStatus(null);
        initialLog.setToStatus("OPENED");
        initialLog.setChangedBy(master);
        statusLogRepository.save(initialLog);

        log.info("Заказ-наряд #{} успешно открыт мастером ID: {}", savedOrder.getId(), master.getId());

        return workOrderMapper.toResponseDto(savedOrder);
    }

    @Transactional
    public void changeOrderStatus(Long orderId, String newStatus, Employee employee) {
        WorkOrder order = workOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        workOrderRepository.save(order);

        // аудит-лог в БД для аналитики
        WorkOrderStatusLog statusLog = new WorkOrderStatusLog();
        statusLog.setWorkOrder(order);
        statusLog.setFromStatus(oldStatus);
        statusLog.setToStatus(newStatus);
        statusLog.setChangedBy(employee);
        statusLogRepository.save(statusLog);
    }

    @Transactional
    public void addServiceToOrder(Long orderId, ServiceCatalog service, Employee mechanic) {
        WorkOrder order = workOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        BigDecimal coefficient = carClassRepository.findByBrand(order.getVehicle().getMake())
                .map(CarClass::getPriceCoefficient)
                .orElse(BigDecimal.ONE);

        BigDecimal finalPrice = service.getHourRatePrice()
                .multiply(service.getNormHours())
                .multiply(coefficient);

        OrderServiceItem item = new OrderServiceItem();
        item.setWorkOrder(order);
        item.setService(service);
        item.setMechanic(mechanic);
        item.setFinalPrice(finalPrice);
        order.getServices().add(item);

        workOrderRepository.save(order);
    }

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Transactional
    public void allocateParts(WorkOrder order) {
        for (OrderPartItem partItem : order.getParts()) {
            int requiredQty = partItem.getQuantity();

            // все доступные партии этой детали, отсортированные по дате прихода (старые — первые)
            List<PartBatch> activeBatches = partBatchRepository
                    .findAvailableBatches(partItem.getPart().getId());

            int totalAvailable = activeBatches.stream().mapToInt(PartBatch::getAvailableQuantity).sum();
            if (totalAvailable < requiredQty) {
                throw new IllegalStateException("Недостаточно остатков для детали: " + partItem.getPart().getName());
            }

            for (PartBatch batch : activeBatches) {
                if (requiredQty <= 0) break;

                int batchAvailable = batch.getAvailableQuantity();
                if (batchAvailable >= requiredQty) {
                    // партия полностью покрывает остаток потребности
                    batch.setAvailableQuantity(batchAvailable - requiredQty);
                    requiredQty = 0;
                } else {
                    // забираем из партии всё, что есть, и идем к следующей
                    requiredQty -= batchAvailable;
                    batch.setAvailableQuantity(0);
                }
                partBatchRepository.save(batch);
            }
        }
    }
}

