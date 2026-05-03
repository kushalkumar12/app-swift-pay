package com.swiftpay.ledger.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.swiftpay.ledger.dto.PaymentEventDTO;
import com.swiftpay.ledger.service.LedgerService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentConsumer {

	private final LedgerService ledgerService;

	@KafkaListener(topics = "payment.initiated", groupId = "ledger-group")
	public void consume(PaymentEventDTO event, Acknowledgment ack) throws Exception {

		try {
			ledgerService.processPayment(event);
			ack.acknowledge();
		} catch (Exception e) {
			System.err.println("Retrying event: " + event.getTransactionId());
			throw e;
		}
	}
}
