package com.swiftpay.gateway.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swiftpay.gateway.model.Transaction;

public interface PaymentRepository extends JpaRepository<Transaction, Long>{
	Optional<Transaction> findByTransactionId(String transactionId);
}
