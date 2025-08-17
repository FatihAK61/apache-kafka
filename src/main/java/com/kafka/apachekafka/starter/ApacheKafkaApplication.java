package com.kafka.apachekafka.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.kafka.apachekafka"})
public class ApacheKafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApacheKafkaApplication.class, args);
    }

}
