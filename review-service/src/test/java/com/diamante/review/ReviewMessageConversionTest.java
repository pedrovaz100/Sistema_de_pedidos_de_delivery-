package com.diamante.review;

import com.diamante.review.dto.ReviewMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewMessageConversionTest {

    @Test
    void shouldConvertJsonPublishedByAnotherServiceIntoReviewMessage() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        String json = "{\"dishId\":1,\"dishName\":\"House Burger\",\"rating\":5,\"comment\":\"Great\"}";
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setHeader("__TypeId__", "com.diamante.order.dto.ReviewMessage")
                .build();
        message.getMessageProperties().setInferredArgumentType(ReviewMessage.class);

        Object converted = converter.fromMessage(message);

        assertThat(converted).isEqualTo(new ReviewMessage(1L, "House Burger", 5, "Great"));
    }
}
