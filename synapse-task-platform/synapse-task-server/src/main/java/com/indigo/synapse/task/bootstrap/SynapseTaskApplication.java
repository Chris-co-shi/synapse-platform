package com.indigo.synapse.task.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.task")
public class SynapseTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseTaskApplication.class, args);
    }
}
