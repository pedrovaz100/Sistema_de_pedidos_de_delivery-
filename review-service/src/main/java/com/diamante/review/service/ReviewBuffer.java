package com.diamante.review.service;

import com.diamante.review.dto.ReviewMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReviewBuffer {

    private final ConcurrentHashMap<Long, ReviewAccumulator> buffer = new ConcurrentHashMap<>();

    public void add(ReviewMessage message) {
        buffer.merge(
                message.dishId(),
                ReviewAccumulator.of(message.dishName(), message.rating()),
                ReviewAccumulator::merge);
    }

    public void restore(Map<Long, ReviewAccumulator> drained) {
        drained.forEach((dishId, accumulator) -> buffer.merge(dishId, accumulator, ReviewAccumulator::merge));
    }

    public Map<Long, ReviewAccumulator> drain() {
        Map<Long, ReviewAccumulator> drained = new HashMap<>();
        for (Long dishId : buffer.keySet()) {
            ReviewAccumulator accumulator = buffer.remove(dishId);
            if (accumulator != null) {
                drained.put(dishId, accumulator);
            }
        }
        return drained;
    }

    public int size() {
        return buffer.size();
    }
}
