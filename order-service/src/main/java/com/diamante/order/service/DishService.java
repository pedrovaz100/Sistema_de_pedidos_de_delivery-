package com.diamante.order.service;

import com.diamante.order.entity.Dish;
import com.diamante.order.exception.DishNotFoundException;
import com.diamante.order.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;

    @Transactional(readOnly = true)
    public List<Dish> findAll() {
        return dishRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Dish findById(Long id) {
        return dishRepository.findById(id).orElseThrow(DishNotFoundException::new);
    }
}
