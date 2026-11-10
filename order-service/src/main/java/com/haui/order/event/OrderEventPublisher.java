package com.haui.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.order}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.order-created}")
    private String routingKey;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent to RabbitMQ [Exchange: {}, RoutingKey: {}] for order: {}",
                exchangeName, routingKey, event.getOrderCode());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
    }
}
