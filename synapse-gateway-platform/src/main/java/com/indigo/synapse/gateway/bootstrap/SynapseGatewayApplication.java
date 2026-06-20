package com.indigo.synapse.gateway.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Synapse Platform Gateway 启动入口。
 *
 * <p>该应用只启用 Reactive Gateway、认证和路由基础设施，不承载业务 Controller 或数据库访问。</p>
 */
@SpringBootApplication(scanBasePackages = "com.indigo.synapse.gateway")
public class SynapseGatewayApplication {

    /**
     * 启动 Synapse Gateway。
     *
     * @param args Spring Boot 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SynapseGatewayApplication.class, args);
    }
}
