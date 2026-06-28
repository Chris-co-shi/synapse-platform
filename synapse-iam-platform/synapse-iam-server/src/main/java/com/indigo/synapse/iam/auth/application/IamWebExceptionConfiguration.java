package com.indigo.synapse.iam.auth.application;

import com.indigo.synapse.webmvc.exception.GlobalExceptionHandler;
import com.indigo.synapse.webmvc.exception.WebExceptionResponseFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IAM Web 异常响应配置。
 *
 * <p>Framework 的 {@link GlobalExceptionHandler} 位于 Platform 应用扫描范围之外，
 * IAM 在本服务内显式注册该处理器，复用 Framework 已有的 Result 响应结构、TraceId 和
 * {@link com.indigo.synapse.web.core.error.ErrorHttpStatusResolver} 状态码映射。</p>
 */
@Configuration(proxyBeanMethods = false)
public class IamWebExceptionConfiguration {

    /**
     * 注册 MVC Controller 阶段异常处理器。
     *
     * @param responseFactory Framework MVC 异常响应工厂
     * @return 全局异常处理器
     */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler iamGlobalExceptionHandler(WebExceptionResponseFactory responseFactory) {
        return new GlobalExceptionHandler(responseFactory);
    }
}
