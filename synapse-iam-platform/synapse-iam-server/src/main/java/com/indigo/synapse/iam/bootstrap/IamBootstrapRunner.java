package com.indigo.synapse.iam.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * IAM 启动期本地开发初始化入口。
 */
@Component
@RequiredArgsConstructor
public class IamBootstrapRunner implements ApplicationRunner {

    private final IamBootstrapService bootstrapService;

    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.initialize();
    }
}
