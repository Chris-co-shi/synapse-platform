package com.indigo.synapse.iam.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.iam")
public class SynapseIamApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseIamApplication.class, args);
    }
}
