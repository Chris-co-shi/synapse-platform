package com.indigo.synapse.gateway.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.gateway")
public class SynapseGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseGatewayApplication.class, args);
    }
}
