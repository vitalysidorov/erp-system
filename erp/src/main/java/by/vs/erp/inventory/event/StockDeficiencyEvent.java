package by.vs.erp.inventory.event;

public record StockDeficiencyEvent(
        Long partId,
        String oemNumber,
        String partName,
        int remainingQuantity,
        int minLimit
) {}