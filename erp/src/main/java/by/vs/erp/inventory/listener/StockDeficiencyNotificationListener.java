package by.vs.erp.inventory.listener;

import by.vs.erp.inventory.event.StockDeficiencyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static by.vs.erp.inventory.config.RabbitMQConfig.STOCK_DEFICIENCY_QUEUE;

@Component
@Slf4j
public class StockDeficiencyNotificationListener {

    // autoStartup управляется свойством, чтобы в тестах слушатель не пытался поднять
    // соединение с RabbitMQ при старте контекста.
    @RabbitListener(queues = STOCK_DEFICIENCY_QUEUE,
            autoStartup = "${rabbitmq.listener.auto-start:true}")
    public void handleStockDeficiency(StockDeficiencyEvent event) {
        // TODO: интеграция с реальным каналом уведомлений (email/Slack для менеджера склада)
        log.warn("АВТОЗАКАЗ: деталь '{}' (OEM: {}) требует пополнения. Осталось: {} шт. (порог: {})",
                event.partName(), event.oemNumber(), event.remainingQuantity(), event.minLimit());
    }
}