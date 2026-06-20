package com.indigo.synapse.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Synapse Audit 服务启动入口。
 *
 * <p>启动类位于领域根包，由 Spring Boot 默认扫描本模块组件。</p>
 */
@SpringBootApplication
public class SynapseAuditApplication {

    /**
     * 启动 Synapse Audit 服务。
     *
     * @param args Spring Boot 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SynapseAuditApplication.class, args);
    }
}
