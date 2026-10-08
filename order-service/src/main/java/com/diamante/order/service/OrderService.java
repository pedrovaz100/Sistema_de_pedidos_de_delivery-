package com.diamante.order.service;

import com.diamante.order.client.PaymentClient;
import com.diamante.order.dto.CreateOrderRequest;
import com.diamante.order.entity.CustomerOrder;
import com.diamante.order.entity.Dish;
import com.diamante.order.entity.OrderStatus;
import com.diamante.order.exception.DishNotFoundException;
import com.diamante.order.exception.OrderNotFoundException;
import com.diamante.order.exception.OutOfStockException;
import com.diamante.order.exception.PaymentFailedException;
import com.diamante.order.repository.CustomerOrderRepository;
import com.diamante.order.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final DishRepository dishRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentClient paymentClient;

    @Transactional
    public CustomerOrder createOrder(CreateOrderRequest request) {
        Dish dish = dishRepository.findByIdForUpdate(request.dishId())
                .orElseThrow(DishNotFoundException::new);

        if (dish.getStock() < request.quantity()) {
            throw new OutOfStockException();
        }

        BigDecimal totalPrice = dish.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

        try {
            paymentClient.charge(totalPrice);
        } catch (RuntimeException exception) {
            log.error("Payment definitively failed for dish {}, stock left untouched", dish.getId());
            throw new PaymentFailedException(exception);
        }

        dish.setStock(dish.getStock() - request.quantity());

        return customerOrderRepository.save(CustomerOrder.builder()
                .dishId(dish.getId())
                .quantity(request.quantity())
                .totalPrice(totalPrice)
                .status(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build());
    }

    @Transactional(readOnly = true)
    public CustomerOrder findById(Long id) {
        return customerOrderRepository.findById(id).orElseThrow(OrderNotFoundException::new);
    }
}
