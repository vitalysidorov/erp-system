package by.vs.erp.order.event;

import by.vs.erp.order.entity.WorkOrder;

import java.math.BigDecimal;

public record ClosedWorkOrderEvent(WorkOrder order, BigDecimal finalRevenue) {
}
