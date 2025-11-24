package com.mtmn.smartdoc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Knife4j 接口文档配置
 * <p>
 * 配置说明：
 * 1. OpenAPI 3.0 规范文档配置
 * 2. JWT 认证安全配置
 * 3. API 分组管理
 * 4. 服务器环境配置
 *
 * @author CharmingDaiDai
 * @since 2025-04-15
 */
@Configuration
public class Knife4jConfig {

    /**
     * 全局 OpenAPI 配置
     * <p>
     * 配置 API 基本信息、服务器地址、安全认证方案
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // 创建联系人信息
        Contact contact = new Contact()
                .name("CharmingDaiDai")
                .email("admin@mtmn.com")
                .url("https://www.mtmn.com");

        // 创建 API 基本信息
        Info info = new Info()
                .title("智能文档系统 API")
                .description("基于 LangChain4j 的智能文档系统接口文档，支持文档管理、知识库检索、智能对话等功能")
                .version("v1.0.0")
                .contact(contact)
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        // 配置服务器地址
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url("/").description("当前环境"));

        // JWT 认证配置
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("请在下方输入 JWT Token（无需添加 Bearer 前缀）");

        // 全局安全要求
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("JWT认证");

        return new OpenAPI()
                .info(info)
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("JWT认证", securityScheme))
                .addSecurityItem(securityRequirement);
    }

        /**
         * 认证与个人资料接口分组
         * <p>
         * 包含：登录、注册、Token 刷新、个人资料查询与修改等接口
         */
        @Bean
        public GroupedOpenApi authAndProfileApi() {
                return GroupedOpenApi.builder()
                                .group("1. 认证与用户")
                                .pathsToMatch("/api/auth/**", "/api/profile/**")
                                .packagesToScan("com.mtmn.smartdoc.controller")
                                .build();
        }

    /**
     * 文档管理接口分组
     * <p>
     * 包含：文档上传、下载、删除、列表查询等接口
     */
    @Bean
    public GroupedOpenApi documentApi() {
        return GroupedOpenApi.builder()
                .group("2. 文档管理")
                .pathsToMatch("/api/documents/**")
                .packagesToScan("com.mtmn.smartdoc.controller")
                .build();
    }

    /**
     * 知识库接口分组
     * <p>
     * 包含：知识库创建、查询、删除等接口
     */
    @Bean
    public GroupedOpenApi knowledgeBaseApi() {
        return GroupedOpenApi.builder()
                .group("3. 知识库")
                .pathsToMatch("/api/knowledge-bases/**")
                .packagesToScan("com.mtmn.smartdoc.controller")
                .build();
    }

    /**
     * 仪表盘数据接口分组
     * <p>
     * 包含：统计数据、用户活动等接口
     */
    @Bean
    public GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder()
                .group("4. 仪表盘")
                .pathsToMatch("/api/dashboard/**")
                .packagesToScan("com.mtmn.smartdoc.controller")
                .build();
    }

    /**
     * 模型管理接口分组
     * <p>
     * 包含：LLM 和嵌入模型查询等接口
     */
    @Bean
    public GroupedOpenApi modelApi() {
        return GroupedOpenApi.builder()
                .group("5. 模型管理")
                .pathsToMatch("/api/models/**")
                .packagesToScan("com.mtmn.smartdoc.controller")
                .build();
    }
}