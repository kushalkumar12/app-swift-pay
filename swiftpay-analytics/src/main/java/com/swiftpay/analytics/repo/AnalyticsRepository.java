package com.swiftpay.analytics.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swiftpay.analytics.model.PaymentAnalytics;

public interface AnalyticsRepository extends JpaRepository<PaymentAnalytics, Long> {

}
