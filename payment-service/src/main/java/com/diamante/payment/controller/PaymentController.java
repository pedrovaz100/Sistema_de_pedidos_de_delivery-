package com.diamante.payment.controller;

import com.diamante.payment.dto.PaymentRequest;
import com.diamante.payment.dto.PaymentResponse;
import com.diamante.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponse process(@Valid @RequestBody PaymentRequest request) {
        return paymentService.process(request);
    }
}
