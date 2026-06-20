package com.indigo.synapse.iam.bootstrap.controller;

import com.indigo.synapse.webmvc.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 史偕成
 * @date 2026/06/17 17:48
 **/

@RestController
public class TestController {

    @GetMapping("/test")
    public Result<Boolean> test() {
        return Result.success(true);
    }
}
