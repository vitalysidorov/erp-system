package by.vs.erp.order.service;

import by.vs.erp.order.dto.MechanicTaskDto;
import by.vs.erp.order.mapper.MechanicTaskMapper;
import by.vs.erp.order.repository.OrderServiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MechanicTaskService {

    private final OrderServiceItemRepository serviceItemRepository;
    private final MechanicTaskMapper mechanicTaskMapper;

    // получить список активных ремонтных задач для конкретного механика
    @Transactional(readOnly = true)
    public List<MechanicTaskDto> getActiveTasksForMechanic(Long mechanicId) {
        return serviceItemRepository.findActiveTasksByMechanic(mechanicId).stream()
                .map(mechanicTaskMapper::toTaskDto)
                .collect(Collectors.toList());
    }
}

