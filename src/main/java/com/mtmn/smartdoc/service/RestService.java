package com.mtmn.smartdoc.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description RestService
 * @date 2025/4/29 09:26
 */
@Service
public class RestService {

    /**
     * 创建RestTemplate Bean
     * <p>
     * 实现思路：
     * 1. 使用RestTemplateBuilder配置RestTemplate实例
     * 2. 设置连接超时时间为10秒，防止连接挂起
     * 3. 设置读取超时时间为10秒，防止读取响应时挂起
     * 4. 构建并返回配置完成的RestTemplate实例
     * 5. 该Bean可在整个应用中注入使用，用于HTTP客户端调用
     *
     * @param builder Spring Boot提供的RestTemplate构建器
     * @return 配置了超时参数的RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}