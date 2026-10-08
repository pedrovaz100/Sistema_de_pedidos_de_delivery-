package com.diamante.order.controller;

import com.diamante.order.dto.AssistantRequest;
import com.diamante.order.dto.AssistantResponse;
import com.diamante.order.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final ChatService chatService;

    @PostMapping
    public AssistantResponse ask(@Valid @RequestBody AssistantRequest request) {
        return new AssistantResponse(chatService.ask(request.question()));
    }
}
