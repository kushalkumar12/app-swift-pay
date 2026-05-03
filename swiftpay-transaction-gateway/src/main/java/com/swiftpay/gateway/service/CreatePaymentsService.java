package com.swiftpay.gateway.service;

import com.swiftpay.gateway.dto.PaymentReqDTO;
import com.swiftpay.gateway.enums.ValidateUserEnum;

public interface CreatePaymentsService {
	public String createPaymentService(PaymentReqDTO paymentReqDto);
	public ValidateUserEnum validateTransactionUsers(String senderId, String receiverId);
}
