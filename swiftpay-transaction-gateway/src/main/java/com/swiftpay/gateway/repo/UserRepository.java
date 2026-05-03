package com.swiftpay.gateway.repo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swiftpay.gateway.model.User;

public interface UserRepository extends JpaRepository<User, Long>{
	Optional<User> findByUserId(String userId);
	
	@Query("SELECT u.userId FROM User u WHERE u.userId IN :userIds and u.isActive = true")
	Set<String> findExistingUserIds(@Param("userIds") List<String> userIds);
}
