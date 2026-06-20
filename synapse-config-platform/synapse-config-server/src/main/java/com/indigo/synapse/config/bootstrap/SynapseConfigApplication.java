package com.indigo.synapse.config.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.config")
public class SynapseConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseConfigApplication.class, args);
    }
}
