package com.swiftpay.ledger.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swiftpay.ledger.model.Account;
import com.swiftpay.ledger.model.Transaction;
import com.swiftpay.ledger.repo.AccountRepository;
import com.swiftpay.ledger.repo.LedgerRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
@Tag(name = "Transaction Details", description = "Operations related to Transactions")
public class LedgerControllerAction {
	private final LedgerRepository repository;
	private final AccountRepository accountRepository;

	@Operation(summary = "Get User Transaction Details")
	@GetMapping("/transactions/{userId}")
	public List<Transaction> getTransactions(@PathVariable String userId) {
		return repository.findBySenderIdOrReceiverId(userId, userId);
	}

	@Operation(summary = "Get the balance of user")
	@GetMapping("/balance/{userId}")
	public BigDecimal getBalance(@PathVariable String userId) {

		Account sender = accountRepository.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("User not found: " + userId));

		return sender.getBalance();
	}

}
