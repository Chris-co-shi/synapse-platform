package com.indigo.synapse.integration.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.integration")
public class SynapseIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseIntegrationApplication.class, args);
    }
}
