package by.vs.erp.order.controller;

import by.vs.erp.order.dto.MechanicTaskDto;
import by.vs.erp.order.service.MechanicTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mechanic")
@RequiredArgsConstructor
public class MechanicTaskRestController {

    private final MechanicTaskService mechanicTaskService;

    @GetMapping("/tasks/{mechanicId}")
    @PreAuthorize(value = "hasRole('MASTER')")
    public List<MechanicTaskDto> getActiveTasks(@PathVariable Long mechanicId) {
        return mechanicTaskService.getActiveTasksForMechanic(mechanicId);
    }
}


