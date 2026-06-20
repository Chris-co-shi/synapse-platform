package com.indigo.synapse.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Synapse Report 服务启动入口。
 *
 * <p>启动类位于领域根包，由 Spring Boot 默认扫描本模块组件。</p>
 */
@SpringBootApplication
public class SynapseReportApplication {

    /**
     * 启动 Synapse Report 服务。
     *
     * @param args Spring Boot 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SynapseReportApplication.class, args);
    }
}
