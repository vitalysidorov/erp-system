package by.vs.erp.inventory.event;

import java.math.BigDecimal;

public record ReceivedNewBatchEvent(BigDecimal amount) {
}
