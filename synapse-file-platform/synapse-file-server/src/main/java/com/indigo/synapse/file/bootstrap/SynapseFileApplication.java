package com.indigo.synapse.file.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.file")
public class SynapseFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseFileApplication.class, args);
    }
}
