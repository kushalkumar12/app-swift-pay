package com.swiftpay.ledger.serviceimpl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.swiftpay.ledger.dto.PaymentCompletedEvent;
import com.swiftpay.ledger.dto.PaymentEventDTO;
import com.swiftpay.ledger.model.Account;
import com.swiftpay.ledger.model.Transaction;
import com.swiftpay.ledger.repo.AccountRepository;
import com.swiftpay.ledger.repo.LedgerRepository;
import com.swiftpay.ledger.service.LedgerService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

	private final AccountRepository accountRepository;
	private final LedgerRepository ledgerRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final RedisTemplate<String, String> redisTemplate;

	private static final String SUCCESS_TOPIC = "payment.completed";

	@Override
	@Transactional
	public void processPayment(PaymentEventDTO event) {

		String key = "txn:" + event.getTransactionId();
		
		//1. CHECK REDIIS IF NOT ADD THE KEY FOR IDEMPENTENCY
		Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", 5, TimeUnit.MINUTES);

		if (Boolean.FALSE.equals(isNew)) {
			// Already processing or processed -- DUPLICATE
			return;
		}

		try {
			//GET THE DATA FORM THE DB AND VALIDATET THE SENDER AND RECEIVER HAS ACCOUNTS
			
			Account sender = accountRepository.findByUserId(event.getSenderId())
					.orElseThrow(() -> new RuntimeException("Sender not found: " + event.getSenderId()));

			Account receiver = accountRepository.findByUserId(event.getReceiverId())
					.orElseThrow(() -> new RuntimeException("Receiver not found: " + event.getReceiverId()));

			//vALIDATE THE BALANCE SHOULD BE EQUAL FOR MORE THEN
			if (sender.getBalance().compareTo(event.getAmount()) < 0) {
				throw new RuntimeException("Insufficient balance for user: " + event.getSenderId());
			}

			//mAKE  PROCESS OF DEDUCT AND CREDIT
			sender.setBalance(sender.getBalance().subtract(event.getAmount()));
			receiver.setBalance(receiver.getBalance().add(event.getAmount()));

			accountRepository.save(sender);
			accountRepository.save(receiver);

			//INSERT THE RECORD IN DB 
			Transaction txn = ledgerRepository.findByTransactionId(event.getTransactionId())
					.orElseGet(() -> Transaction.builder().transactionId(event.getTransactionId())
							.senderId(event.getSenderId()).receiverId(event.getReceiverId()).amount(event.getAmount())
							.status("COMPLETED").build());

			txn.setStatus("COMPLETED");
			ledgerRepository.save(txn);

			//MARK THE REDIS SERVER THE TRANBSACTION IS SUCCESS
			redisTemplate.opsForValue().set(key, "SUCCESS", 24, TimeUnit.HOURS);

			
			PaymentCompletedEvent pce = new PaymentCompletedEvent();
			pce.setAmount(event.getAmount());
			pce.setTransactionId(event.getTransactionId());
			pce.setCompletedAt(Instant.now());
			pce.setCurrency(event.getCurrency());
			pce.setReceiverId(event.getReceiverId());
			pce.setSenderId(event.getSenderId());

			//SEND FOR ANALYTICS SERVICE
			kafkaTemplate.send(SUCCESS_TOPIC, event.getTransactionId(), pce);

		} catch (Exception ex) {

			//fOR RETRY
			redisTemplate.delete(key);

			throw new RuntimeException("Payment processing failed for txn: " + event.getTransactionId(), ex);
		}
	}
}