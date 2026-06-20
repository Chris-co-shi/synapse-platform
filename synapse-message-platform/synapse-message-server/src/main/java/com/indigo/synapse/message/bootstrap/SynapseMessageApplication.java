package com.indigo.synapse.message.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.message")
public class SynapseMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseMessageApplication.class, args);
    }
}
