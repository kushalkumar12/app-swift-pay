package com.swiftpay.ledger.service;

import com.swiftpay.ledger.dto.PaymentEventDTO;

public interface LedgerService {
	 public void processPayment(PaymentEventDTO event) throws Exception;
}
