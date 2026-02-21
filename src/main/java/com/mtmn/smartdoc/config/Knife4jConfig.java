//package com.mtmn.smartdoc.config;
//
//import io.swagger.v3.oas.models.Components;
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Contact;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.info.License;
//import io.swagger.v3.oas.models.security.SecurityRequirement;
//import io.swagger.v3.oas.models.security.SecurityScheme;
//import io.swagger.v3.oas.models.servers.Server;
//import org.springdoc.core.models.GroupedOpenApi;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Knife4j 接口文档配置
// * <p>
// * 配置说明：
// * 1. OpenAPI 3.0 规范文档配置
// * 2. JWT 认证安全配置
// * 3. API 分组管理
// * 4. 服务器环境配置
// *
// * @author CharmingDaiDai
// * @since 2025-04-15
// */
//@Configuration
//public class Knife4jConfig {
//
//    /**
//     * 全局 OpenAPI 配置
//     * <p>
//     * 配置 API 基本信息、服务器地址、安全认证方案
//     */
//    @Bean
//    public OpenAPI customOpenAPI() {
//        // 创建联系人信息
//        Contact contact = new Contact()
//                .name("CharmingDaiDai")
//                .email("admin@mtmn.com")
//                .url("https://www.mtmn.com");
//
//        // 创建 API 基本信息
//        Info info = new Info()
//                .title("智能文档系统 API")
//                .description("基于 LangChain4j 的智能文档系统接口文档，支持文档管理、知识库检索、智能对话等功能")
//                .version("v1.0.0")
//                .contact(contact)
//                .license(new License()
//                        .name("MIT License")
//                        .url("https://opensource.org/licenses/MIT"));
//
//        // 配置服务器地址
//        List<Server> servers = new ArrayList<>();
//        servers.add(new Server().url("/").description("当前环境"));
//
//        // JWT 认证配置 - Bearer 模式会自动添加 "Bearer " 前缀
//        SecurityScheme securityScheme = new SecurityScheme()
//                .name("Authorization")
//                .type(SecurityScheme.Type.HTTP)
//                .scheme("bearer")
//                .bearerFormat("JWT")
//                .in(SecurityScheme.In.HEADER)
//                .description("请输入 JWT Token（系统会自动添加 Bearer 前缀，只需粘贴 token 本身）");
//
//        // 全局安全要求
//        SecurityRequirement securityRequirement = new SecurityRequirement()
//                .addList("BearerAuth");
//
//        return new OpenAPI()
//                .info(info)
//                .servers(servers)
//                .components(new Components()
//                        .addSecuritySchemes("BearerAuth", securityScheme))
//                .addSecurityItem(securityRequirement);
//    }
//}