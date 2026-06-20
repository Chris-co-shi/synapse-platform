package com.indigo.synapse.mdm.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.mdm")
public class SynapseMdmApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseMdmApplication.class, args);
    }
}
