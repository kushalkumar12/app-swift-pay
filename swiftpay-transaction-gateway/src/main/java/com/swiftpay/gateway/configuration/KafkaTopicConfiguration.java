package com.swiftpay.gateway.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfiguration {
	@Bean
    public NewTopic paymentTopic() {
        return new NewTopic("payment.initiated", 3, (short) 1);
    }
}
