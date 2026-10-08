package com.diamante.order;

import com.diamante.order.client.PaymentClient;
import com.diamante.order.dto.PaymentResponse;
import com.diamante.order.entity.Dish;
import com.diamante.order.messaging.ReviewPublisher;
import com.diamante.order.repository.CustomerOrderRepository;
import com.diamante.order.repository.DishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected DishRepository dishRepository;

    @Autowired
    protected CustomerOrderRepository customerOrderRepository;

    @MockitoBean
    protected PaymentClient paymentClient;

    @MockitoBean
    protected ReviewPublisher reviewPublisher;

    @MockitoBean
    protected ChatModel chatModel;

    @BeforeEach
    void resetState() {
        reset(paymentClient, reviewPublisher, chatModel);
        customerOrderRepository.deleteAll();
        dishRepository.deleteAll();
        when(paymentClient.charge(any())).thenReturn(new PaymentResponse("APPROVED", 8081));
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
    }

    protected Dish saveDish(String name, String price, int stock) {
        return dishRepository.save(Dish.builder()
                .name(name)
                .description(name + " description")
                .price(new BigDecimal(price))
                .stock(stock)
                .build());
    }
}
