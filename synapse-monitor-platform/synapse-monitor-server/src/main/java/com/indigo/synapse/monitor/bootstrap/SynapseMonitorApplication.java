package com.indigo.synapse.monitor.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indigo.synapse.monitor")
public class SynapseMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseMonitorApplication.class, args);
    }
}
