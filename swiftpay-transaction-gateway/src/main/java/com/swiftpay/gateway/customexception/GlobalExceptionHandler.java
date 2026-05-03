package com.swiftpay.gateway.customexception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {

		List<Map<String, String>> errors = new ArrayList<>();

		ex.getBindingResult().getFieldErrors().forEach(error -> {
			Map<String, String> err = new HashMap<>();
			err.put("field", error.getField());
			err.put("message", error.getDefaultMessage());
			errors.add(err);
		});

		Map<String, Object> response = new HashMap<>();
		response.put("status", "FAILED");
		response.put("message", "Validation error");
		response.put("errors", errors);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleInvalidJson(HttpMessageNotReadableException ex) {

		return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid request body"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception ex) {
		return ResponseEntity.internalServerError().body(ex.getMessage());
	}
}
