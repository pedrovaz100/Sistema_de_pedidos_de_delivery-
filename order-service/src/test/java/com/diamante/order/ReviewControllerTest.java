package com.diamante.order;

import com.diamante.order.dto.ReviewMessage;
import com.diamante.order.entity.Dish;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest extends AbstractIntegrationTest {

    private String reviewBody(Long dishId, int rating) {
        return "{\"dishId\": %d, \"rating\": %d, \"comment\": \"Great\"}".formatted(dishId, rating);
    }

    @Test
    void shouldAcceptValidReviewAndPublishMessage() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);

        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(reviewBody(dish.getId(), 5)))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        ArgumentCaptor<ReviewMessage> captor = ArgumentCaptor.forClass(ReviewMessage.class);
        verify(reviewPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new ReviewMessage(dish.getId(), "House Burger", 5, "Great"));
    }

    @Test
    void shouldAcceptBoundaryRatings() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);

        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(reviewBody(dish.getId(), 1)))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(reviewBody(dish.getId(), 5)))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldRejectRatingAboveFive() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);

        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(reviewBody(dish.getId(), 6)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("rating must be between 1 and 5"));

        verify(reviewPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectRatingBelowOne() throws Exception {
        Dish dish = saveDish("House Burger", "39.90", 10);

        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(reviewBody(dish.getId(), 0)))
                .andExpect(status().isBadRequest());

        verify(reviewPublisher, never()).publish(any());
    }

    @Test
    void shouldReturnNotFoundForUnknownDish() throws Exception {
        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(reviewBody(999999L, 4)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Dish not found"));

        verify(reviewPublisher, never()).publish(any());
    }
}
