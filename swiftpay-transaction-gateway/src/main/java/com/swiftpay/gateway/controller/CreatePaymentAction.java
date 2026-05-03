package com.swiftpay.gateway.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swiftpay.gateway.dto.PaymentReqDTO;
import com.swiftpay.gateway.service.CreatePaymentsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Payment APIs", description = "Operations related to payments")
public class CreatePaymentAction {

	private final CreatePaymentsService paymentService;

	public CreatePaymentAction(CreatePaymentsService paymentService) {
		this.paymentService = paymentService;
	}

	@Operation(summary = "Create a payment transaction")
	@PostMapping
	public String createPaymentRequest(@Valid @RequestBody PaymentReqDTO reqPaymentDtls) {

		return paymentService.createPaymentService(reqPaymentDtls);
	}
}
