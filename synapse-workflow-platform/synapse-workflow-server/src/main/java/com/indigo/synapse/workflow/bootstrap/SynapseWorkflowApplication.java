package com.indigo.synapse.workflow.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.workflow")
public class SynapseWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseWorkflowApplication.class, args);
    }
}
