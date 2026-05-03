package com.swiftpay.analytics.service;

import com.swiftpay.analytics.dto.PaymentCompletedEvent;

public interface AnalyticsService {
	public void saveEvent(PaymentCompletedEvent event);
}
