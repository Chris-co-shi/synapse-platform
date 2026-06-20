package com.indigo.synapse.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Synapse Workflow 服务启动入口。
 *
 * <p>启动类位于领域根包，由 Spring Boot 默认扫描本模块组件。</p>
 */
@SpringBootApplication
public class SynapseWorkflowApplication {

    /**
     * 启动 Synapse Workflow 服务。
     *
     * @param args Spring Boot 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SynapseWorkflowApplication.class, args);
    }
}
