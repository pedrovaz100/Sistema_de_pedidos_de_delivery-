package com.diamante.order.service;

import com.diamante.order.dto.CreateReviewRequest;
import com.diamante.order.dto.ReviewMessage;
import com.diamante.order.entity.Dish;
import com.diamante.order.exception.DishNotFoundException;
import com.diamante.order.messaging.ReviewPublisher;
import com.diamante.order.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final DishRepository dishRepository;
    private final ReviewPublisher reviewPublisher;

    public void submit(CreateReviewRequest request) {
        Dish dish = dishRepository.findById(request.dishId()).orElseThrow(DishNotFoundException::new);
        reviewPublisher.publish(new ReviewMessage(dish.getId(), dish.getName(), request.rating(), request.comment()));
    }
}
