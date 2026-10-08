package com.diamante.order;

import com.diamante.order.entity.Dish;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest extends AbstractIntegrationTest {

    private String orderBody(Long dishId, int quantity) {
        return "{\"dishId\": %d, \"quantity\": %d}".formatted(dishId, quantity);
    }

    @Test
    void shouldCreateConfirmedOrderAndDecrementStock() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(dish.getId(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.dishId").value(dish.getId()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(79.80))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));

        assertThat(dishRepository.findById(dish.getId()).orElseThrow().getStock()).isEqualTo(8);
        verify(paymentClient).charge(eq(new BigDecimal("79.80")));
    }

    @Test
    void shouldRejectQuantityLowerThanOne() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(dish.getId(), 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("quantity must be at least 1"));

        verify(paymentClient, never()).charge(any());
        assertThat(dishRepository.findById(dish.getId()).orElseThrow().getStock()).isEqualTo(10);
    }

    @Test
    void shouldRejectMalformedBody() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnNotFoundForUnknownDish() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(999999L, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Dish not found"));
    }

    @Test
    void shouldReturnConflictWhenStockIsInsufficient() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 1);

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(dish.getId(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Dish out of stock"));

        verify(paymentClient, never()).charge(any());
        assertThat(dishRepository.findById(dish.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void shouldReturnBadGatewayAndKeepStockWhenPaymentFails() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);
        when(paymentClient.charge(any())).thenThrow(new IllegalStateException("payment unavailable"));

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(dish.getId(), 3)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").exists());

        assertThat(dishRepository.findById(dish.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThat(customerOrderRepository.count()).isZero();
    }

    @Test
    void shouldFindOrderById() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(dish.getId(), 1)))
                .andExpect(status().isCreated());
        Long orderId = customerOrderRepository.findAll().getFirst().getId();

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPrice").value(39.90));
    }

    @Test
    void shouldReturnNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(get("/orders/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found"));
    }
}
