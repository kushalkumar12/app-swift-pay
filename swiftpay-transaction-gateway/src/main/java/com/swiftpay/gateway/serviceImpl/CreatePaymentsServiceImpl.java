package com.swiftpay.gateway.serviceImpl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.swiftpay.gateway.customexception.DuplicateTransactionException;
import com.swiftpay.gateway.dto.PaymentEventDTO;
import com.swiftpay.gateway.dto.PaymentReqDTO;
import com.swiftpay.gateway.enums.ValidateUserEnum;
import com.swiftpay.gateway.model.Transaction;
import com.swiftpay.gateway.repo.PaymentRepository;
import com.swiftpay.gateway.repo.UserRepository;
import com.swiftpay.gateway.service.CreatePaymentsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreatePaymentsServiceImpl implements CreatePaymentsService {

	private final UserRepository userRepository;
	private final RedisTemplate<String, String> redisTemplate;
	private final PaymentRepository paymentRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final RestTemplate restTemplate;

	@Value("${ledger.service.url}")
	private String ledgerServiceUrl;

	private static final String TXN_PREFIX = "txn:";
	private static final String PAYMENT_TOPIC = "payment.initiated";

	public String createPaymentService(PaymentReqDTO paymentReqDto) {

		// ##1. Generating transaction key and saving it in redis
		String txnKey = TXN_PREFIX + paymentReqDto.getTransactionId();

		// ##2. Check for the transaction existed or not
		Boolean isNew = redisTemplate.opsForValue().setIfAbsent(txnKey, "INIT", Duration.ofHours(24));

		if (Boolean.FALSE.equals(isNew)) {
			throw new DuplicateTransactionException("Duplicate transaction request");
		}

		// ##3.Validate both user
		if (paymentReqDto.getSenderId().equals(paymentReqDto.getReceiverId())) {
			return "Both are same users!. Please change receiver or render";
		}

		ValidateUserEnum status = validateTransactionUsers(paymentReqDto.getSenderId(), paymentReqDto.getReceiverId());

		if (status != ValidateUserEnum.VALID) {
			return mapToException(status);
		}

		// ##4. check the balance in bank
		String balanceStatus = checkBalance(paymentReqDto.getSenderId(), paymentReqDto.getAmount());

		if (!"VALID".equals(balanceStatus)) {
			return balanceStatus;
		}

		// ##5. Generate the transaction record
		Transaction txn = Transaction.builder().transactionId(paymentReqDto.getTransactionId())
				.senderId(paymentReqDto.getSenderId()).receiverId(paymentReqDto.getReceiverId())
				.amount(paymentReqDto.getAmount()).currency(paymentReqDto.getCurrency()).status("PENDING")
				.createdAt(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())).build();

		// ##6. insert in the transaction gateways DB.
		paymentRepository.save(txn);

		PaymentEventDTO event = new PaymentEventDTO();
		event.setTransactionId(paymentReqDto.getTransactionId());
		event.setReceiverId(paymentReqDto.getReceiverId());
		event.setSenderId(paymentReqDto.getSenderId());
		event.setAmount(paymentReqDto.getAmount());
		event.setCurrency(paymentReqDto.getCurrency());

		// 5. Send to Kafka topic
		kafkaTemplate.send(PAYMENT_TOPIC, paymentReqDto.getTransactionId(), event);

		return "Payment initiated successfully";

	}

	private String checkBalance(String senderId, BigDecimal amount) {

		String url = ledgerServiceUrl + "/ledger/balance/" + senderId;

		BigDecimal balance = restTemplate.getForObject(url, BigDecimal.class);

		if (balance == null) {
			return "Ledger service unavailable";
		}

		if (balance.compareTo(amount) < 0) {
			return "Insufficient balance";
		}

		return "VALID";
	}

	private String mapToException(ValidateUserEnum status) {
		return switch (status) {
		case SENDER_NOT_FOUND -> "Sender not found";
		case RECEIVER_NOT_FOUND -> "Receiver not found";
		case BOTH_NOT_FOUND -> "Both users not found";
		default -> "Validation error";
		};
	}

	public ValidateUserEnum validateTransactionUsers(String senderId, String receiverId) {

		Set<String> users = userRepository.findExistingUserIds(List.of(senderId, receiverId));

		boolean senderExists = users.contains(senderId);
		boolean receiverExists = users.contains(receiverId);

		if (senderExists && receiverExists) {
			return ValidateUserEnum.VALID;
		}

		if (!senderExists && !receiverExists) {
			return ValidateUserEnum.BOTH_NOT_FOUND;
		}

		if (!senderExists) {
			return ValidateUserEnum.SENDER_NOT_FOUND;
		}

		return ValidateUserEnum.RECEIVER_NOT_FOUND;
	}
}