package com.swiftpay.ledger.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swiftpay.ledger.model.Transaction;

public interface LedgerRepository extends JpaRepository<Transaction, Long> {
	List<Transaction> findBySenderIdOrReceiverId(String senderId, String receiverId);

	Optional<Transaction> findByTransactionId(String transactionId);
}
