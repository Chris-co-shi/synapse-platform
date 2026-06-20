package com.indigo.synapse.iam.bootstrap.controller;

import com.indigo.synapse.web.core.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 服务连通性测试接口。
 *
 * <p>该接口仅保留现有测试用途，不承载 Token 签发或身份认证业务。</p>
 *
 * @author 史偕成
 * @since 2026-06-17
 */
@RestController
public class TestController {

    /**
     * 返回 IAM 服务的基础连通状态。
     *
     * @return 包含成功状态的统一响应
     */
    @GetMapping("/test")
    public Result<Boolean> test() {
        return Result.success(true);
    }
}
