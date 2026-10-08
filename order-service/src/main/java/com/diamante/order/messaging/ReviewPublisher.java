package com.diamante.order.messaging;

import com.diamante.order.config.RabbitConfig;
import com.diamante.order.dto.ReviewMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(ReviewMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.REVIEWS_ROUTING_KEY, message);
    }
}
