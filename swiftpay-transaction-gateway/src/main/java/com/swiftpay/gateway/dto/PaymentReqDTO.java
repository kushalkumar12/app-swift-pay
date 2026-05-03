package com.swiftpay.gateway.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReqDTO {
    
    @JsonProperty("transaction_id")
    @NotBlank(message = "Transaction ID cannot be empty")
    private String transactionId;
    
    @JsonProperty("sender_id")
    @NotBlank(message = "Sender ID is essential")
    private String senderId;

    @JsonProperty("receiver_id")
    @NotBlank(message = "Receiver ID is essential")
    private String receiverId;

    @NotNull(message = "Amount is essential")
    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be in UpperCase 3-letter code (e.g., INR, USD)")
    private String currency;
}