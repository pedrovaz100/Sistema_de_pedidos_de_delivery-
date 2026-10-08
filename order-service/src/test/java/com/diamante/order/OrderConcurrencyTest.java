package com.diamante.order;

import com.diamante.order.entity.Dish;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class OrderConcurrencyTest extends AbstractIntegrationTest {

    private static final int REQUESTS = 50;
    private static final int INITIAL_STOCK = 10;

    @Test
    void shouldConfirmExactlyTenOrdersWhenFiftyRequestsCompeteForTenUnits() throws Exception {
        Dish dish = saveDish("Promo Burger", "39.90", INITIAL_STOCK);
        String body = "{\"dishId\": %d, \"quantity\": 1}".formatted(dish.getId());
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(REQUESTS);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            Callable<Integer> request = () -> {
                startSignal.await();
                return mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            };
            futures.add(executor.submit(request));
        }
        startSignal.countDown();

        int created = 0;
        int conflicts = 0;
        for (Future<Integer> future : futures) {
            int status = future.get();
            if (status == 201) {
                created++;
            } else if (status == 409) {
                conflicts++;
            }
        }
        executor.shutdown();

        assertThat(created).isEqualTo(INITIAL_STOCK);
        assertThat(conflicts).isEqualTo(REQUESTS - INITIAL_STOCK);
        assertThat(customerOrderRepository.count()).isEqualTo(INITIAL_STOCK);
        assertThat(dishRepository.findById(dish.getId()).orElseThrow().getStock()).isZero();
    }
}
