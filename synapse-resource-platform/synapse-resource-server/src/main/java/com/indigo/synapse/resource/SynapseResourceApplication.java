package com.indigo.synapse.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Synapse Resource 服务启动入口。
 *
 * <p>启动类位于领域根包，由 Spring Boot 默认扫描本模块组件。</p>
 */
@SpringBootApplication
public class SynapseResourceApplication {

    /**
     * 启动 Synapse Resource 服务。
     *
     * @param args Spring Boot 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SynapseResourceApplication.class, args);
    }
}
