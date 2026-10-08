package com.diamante.order.config;

import com.diamante.order.entity.Dish;
import com.diamante.order.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final DishRepository dishRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (dishRepository.count() > 0) {
            return;
        }
        List<Dish> dishes = List.of(
                dish("House Burger", "Brioche bun, beef patty, cheddar and caramelized onions", "39.90", 10),
                dish("Margherita Pizza", "Vegetarian pizza with tomato sauce, mozzarella and fresh basil", "45.00", 25),
                dish("Vegan Buddha Bowl", "Vegan bowl with quinoa, roasted vegetables, chickpeas and tahini", "36.50", 20),
                dish("Caesar Salad", "Romaine lettuce, grilled chicken, parmesan and croutons", "32.00", 15),
                dish("Truffle Fries", "Vegetarian crispy fries with truffle oil and parmesan", "24.90", 40),
                dish("Chocolate Brownie", "Vegetarian warm brownie with vanilla ice cream", "18.00", 30),
                dish("Fresh Lemonade", "Vegan lemonade with mint, served cold", "12.00", 50)
        );
        dishRepository.saveAll(dishes);
        log.info("Loaded {} dishes into the menu", dishes.size());
    }

    private Dish dish(String name, String description, String price, int stock) {
        return Dish.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .stock(stock)
                .build();
    }
}
