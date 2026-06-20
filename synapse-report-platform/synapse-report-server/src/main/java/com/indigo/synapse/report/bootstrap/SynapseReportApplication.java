package com.indigo.synapse.report.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.report")
public class SynapseReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseReportApplication.class, args);
    }
}
