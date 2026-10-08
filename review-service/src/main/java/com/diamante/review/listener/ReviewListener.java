package com.diamante.review.listener;

import com.diamante.review.config.RabbitConfig;
import com.diamante.review.dto.ReviewMessage;
import com.diamante.review.service.ReviewBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewListener {

    private final ReviewBuffer reviewBuffer;

    @RabbitListener(queues = RabbitConfig.REVIEWS_QUEUE)
    public void receive(ReviewMessage message) {
        reviewBuffer.add(message);
    }
}
