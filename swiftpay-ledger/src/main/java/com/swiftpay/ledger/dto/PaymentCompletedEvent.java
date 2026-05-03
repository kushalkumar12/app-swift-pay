package com.swiftpay.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {
	private String transactionId;
	private String senderId;
	private String receiverId;
	private BigDecimal amount;
	private String currency;
	private Instant completedAt;
}
