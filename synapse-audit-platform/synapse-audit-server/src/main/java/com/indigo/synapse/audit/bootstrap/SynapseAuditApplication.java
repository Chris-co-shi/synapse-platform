package com.indigo.synapse.audit.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.audit")
public class SynapseAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseAuditApplication.class, args);
    }
}
