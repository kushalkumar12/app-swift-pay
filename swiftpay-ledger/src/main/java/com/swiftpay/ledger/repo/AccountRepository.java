package com.swiftpay.ledger.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swiftpay.ledger.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long>{
	Optional<Account> findByUserId(String userId);
}
