package com.swiftpay.gateway.model;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long userSeqId;

	@Column(nullable = false, length = 20)
	private String userId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "is_active", nullable = false)
	private boolean isActive;

	@CreationTimestamp
	@Column(name = "created_on", nullable = false, updatable = false)
	private OffsetDateTime createdOn;

	@UpdateTimestamp
	@Column(name = "updated_on", nullable = false)
	private OffsetDateTime updateOn;

}
