package com.indigo.synapse.iam.bootstrap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * IAM 本地开发初始化配置注册。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IamBootstrapProperties.class)
public class IamBootstrapConfiguration {
}
