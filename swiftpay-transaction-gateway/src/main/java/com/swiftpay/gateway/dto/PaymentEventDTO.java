package com.swiftpay.gateway.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEventDTO {

	private String transactionId;
	private String senderId;
	private String receiverId;
	private BigDecimal amount;
	private String currency;
}
