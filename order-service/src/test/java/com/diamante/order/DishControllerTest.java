package com.diamante.order;

import com.diamante.order.entity.Dish;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DishControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldListAllDishes() throws Exception {
        saveDish("House Burger", "39.90", 10);
        saveDish("Margherita Pizza", "45.00", 25);

        mockMvc.perform(get("/dishes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("House Burger"))
                .andExpect(jsonPath("$[0].price").value(39.90))
                .andExpect(jsonPath("$[0].stock").value(10));
    }

    @Test
    void shouldFindDishById() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);

        mockMvc.perform(get("/dishes/{id}", dish.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dish.getId()))
                .andExpect(jsonPath("$.name").value("House Burger"))
                .andExpect(jsonPath("$.description").value("House Burger description"))
                .andExpect(jsonPath("$.price").value(39.90))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void shouldReturnNotFoundForUnknownDish() throws Exception {
        mockMvc.perform(get("/dishes/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Dish not found"));
    }

    @Test
    void shouldReturnBadRequestForNonNumericId() throws Exception {
        mockMvc.perform(get("/dishes/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
