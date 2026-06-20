package com.indigo.synapse.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Synapse IAM 服务启动入口。
 *
 * <p>启动类位于领域根包，使 Spring Boot 默认组件扫描覆盖 IAM 领域代码，无需额外指定扫描范围。</p>
 */
@SpringBootApplication
public class SynapseIamApplication {

    /**
     * 启动 Synapse IAM 服务。
     *
     * @param args Spring Boot 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SynapseIamApplication.class, args);
    }
}
