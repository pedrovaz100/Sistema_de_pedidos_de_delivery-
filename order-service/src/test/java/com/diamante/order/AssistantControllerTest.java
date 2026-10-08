package com.diamante.order;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantControllerTest extends AbstractIntegrationTest {

    private ChatResponse responseWith(String text) {
        return new ChatResponse(java.util.List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void shouldAnswerRestaurantQuestionUsingCurrentMenuInPrompt() throws Exception {
        saveDish("Vegan Buddha Bowl", "36.50", 20);
        saveDish("House Burger", "39.90", 0);
        when(chatModel.call(any(Prompt.class))).thenReturn(responseWith("Temos o Vegan Buddha Bowl por R$ 36,50."));

        mockMvc.perform(post("/assistant").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Tem prato vegetariano ate R$ 40?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Temos o Vegan Buddha Bowl por R$ 36,50."));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();
        Message system = prompt.getInstructions().stream()
                .filter(message -> message.getMessageType() == MessageType.SYSTEM)
                .findFirst()
                .orElseThrow();
        assertThat(system.getText())
                .contains("restaurant")
                .contains("Brazilian Portuguese")
                .contains("Vegan Buddha Bowl | R$ 36,50 | stock: 20")
                .contains("House Burger | R$ 39,90 | stock: 0");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("Tem prato vegetariano ate R$ 40?");
    }

    @Test
    void shouldInstructModelToRefuseOffTopicQuestions() throws Exception {
        saveDish("House Burger", "39.90", 10);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(responseWith("Desculpe, so posso ajudar com assuntos do restaurante e do cardapio."));

        mockMvc.perform(post("/assistant").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Quem ganhou a Copa de 2002?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Desculpe, so posso ajudar com assuntos do restaurante e do cardapio."));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        String systemText = captor.getValue().getInstructions().stream()
                .filter(message -> message.getMessageType() == MessageType.SYSTEM)
                .map(Message::getText)
                .findFirst()
                .orElseThrow();
        assertThat(systemText).contains("politely refuse");
    }

    @Test
    void shouldRejectBlankQuestion() throws Exception {
        mockMvc.perform(post("/assistant").contentType(MediaType.APPLICATION_JSON).content("{\"question\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("question is required"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenModelFails() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("model offline"));

        mockMvc.perform(post("/assistant").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Tem opcao vegana?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Assistant is currently unavailable"));
    }
}
