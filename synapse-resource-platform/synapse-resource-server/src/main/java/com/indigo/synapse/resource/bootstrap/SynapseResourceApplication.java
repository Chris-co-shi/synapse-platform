package com.indigo.synapse.resource.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.resource")
public class SynapseResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseResourceApplication.class, args);
    }
}
