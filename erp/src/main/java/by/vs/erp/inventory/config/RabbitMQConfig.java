package by.vs.erp.inventory.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String STOCK_DEFICIENCY_EXCHANGE = "erp.inventory.exchange";
    public static final String STOCK_DEFICIENCY_QUEUE = "erp.inventory.stock-deficiency";
    public static final String STOCK_DEFICIENCY_ROUTING_KEY = "stock.deficiency";

    @Bean
    public DirectExchange inventoryExchange() {
        return new DirectExchange(STOCK_DEFICIENCY_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockDeficiencyQueue() {
        return new Queue(STOCK_DEFICIENCY_QUEUE, true);
    }

    @Bean
    public Binding stockDeficiencyBinding(Queue stockDeficiencyQueue, DirectExchange inventoryExchange) {
        return BindingBuilder.bind(stockDeficiencyQueue)
                .to(inventoryExchange)
                .with(STOCK_DEFICIENCY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}