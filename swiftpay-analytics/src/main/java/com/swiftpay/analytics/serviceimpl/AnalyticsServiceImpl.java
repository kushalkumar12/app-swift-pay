package com.swiftpay.analytics.serviceimpl;

import org.springframework.stereotype.Service;

import com.swiftpay.analytics.dto.PaymentCompletedEvent;
import com.swiftpay.analytics.model.PaymentAnalytics;
import com.swiftpay.analytics.repo.AnalyticsRepository;
import com.swiftpay.analytics.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
	private final AnalyticsRepository repository;

	public void saveEvent(PaymentCompletedEvent event) {

		PaymentAnalytics data = PaymentAnalytics.builder().transactionId(event.getTransactionId())
				.senderId(event.getSenderId()).receiverId(event.getReceiverId()).amount(event.getAmount())
				.currency(event.getCurrency()).completedAt(event.getCompletedAt()).build();

		repository.save(data);
	}
}
