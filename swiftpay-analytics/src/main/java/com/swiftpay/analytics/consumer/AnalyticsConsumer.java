package com.swiftpay.analytics.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.swiftpay.analytics.dto.PaymentCompletedEvent;
import com.swiftpay.analytics.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnalyticsConsumer {
	private final AnalyticsService analyticsService;

	@KafkaListener(topics = "payment.completed", groupId = "analytics-group")
	public void consume(PaymentCompletedEvent event) {
		analyticsService.saveEvent(event);
	}
}
