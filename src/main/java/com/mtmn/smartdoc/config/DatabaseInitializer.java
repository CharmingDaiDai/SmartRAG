package com.mtmn.smartdoc.config;

import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 数据库初始化配置
 * 根据application.yml中的配置决定是否初始化数据库
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${application.database.initialize:false}")
    private boolean shouldInitializeDatabase;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            if (!shouldInitializeDatabase) {
                log.info("数据库初始化已禁用，跳过初始化过程");
                return;
            }

            log.info("开始执行数据库初始化...");

            // 初始化数据库表
            initDatabaseTables();

            // 初始化用户数据
            initializeUsers();

            log.info("数据库初始化完成");
        };
    }

    /**
     * 初始化用户数据
     */
    private void initializeUsers() {
        if (userRepository.count() == 0) {
            log.info("初始化测试用户数据...");

            // 创建普通用户
            User normalUser = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("password"))
                    .email("user@example.com")
                    // .fullName("普通用户")
                    .vip(false)
                    .createdAt(LocalDateTime.now())
                    .enabled(true)
                    .build();

            // 创建VIP用户
            User vipUser = User.builder()
                    .username("vipUser")
                    .password(passwordEncoder.encode("password"))
                    .email("vip@example.com")
                    // .fullName("VIP用户")
                    .vip(true)
                    .createdAt(LocalDateTime.now())
                    .enabled(true)
                    .build();

            // 创建管理员用户
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@example.com")
                    // .fullName("系统管理员")
                    .vip(true)
                    .createdAt(LocalDateTime.now())
                    .enabled(true)
                    .build();

            userRepository.saveAll(List.of(normalUser, vipUser, adminUser));
            log.info("测试用户数据初始化完成");
        } else {
            log.info("用户数据已存在，跳过用户初始化");
        }
    }

    /**
     * 初始化数据库表
     * 按照文件名自动加载并执行db目录下的所有SQL文件
     */
    private void initDatabaseTables() {
        try {
            log.info("开始初始化数据库表...");

            // 获取所有SQL文件并排序
            Resource[] resources = loadSqlResources();
            if (resources == null || resources.length == 0) {
                log.warn("未找到任何SQL文件，跳过表初始化");
                return;
            }

            // 按文件名排序，确保表的创建顺序正确（先创建没有依赖的表）
            Arrays.sort(resources, Comparator.comparing(r -> {
                try {
                    return r.getFilename();
                } catch (Exception e) {
                    return "";
                }
            }));

            // 执行每个SQL文件
            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename == null) continue;

                    log.info("执行SQL文件: {}", filename);
                    String sqlContent = readResourceContent(resource);

                    if (sqlContent != null && !sqlContent.trim().isEmpty()) {
                        // 处理可能包含多条语句的SQL文件
                        String[] statements = sqlContent.split(";");
                        for (String statement : statements) {
                            String trimmedStatement = statement.trim();
                            if (!trimmedStatement.isEmpty() && !trimmedStatement.startsWith("--")) {
                                try {
                                    jdbcTemplate.execute(trimmedStatement);
                                } catch (Exception e) {
                                    // 表可能已经存在，记录异常但继续执行
                                    log.warn("执行SQL语句出现异常，可能表已存在: {}", e.getMessage());
                                }
                            }
                        }
                        log.info("SQL文件 {} 执行成功", filename);
                    }
                } catch (Exception e) {
                    log.error("执行SQL文件失败: {}", resource.getFilename(), e);
                }
            }

            log.info("数据库表初始化完成");
        } catch (Exception e) {
            log.error("初始化数据库表失败", e);
        }
    }

    /**
     * 加载所有SQL资源文件
     */
    private Resource[] loadSqlResources() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            return resolver.getResources("classpath:db/*.sql");
        } catch (IOException e) {
            log.error("加载SQL资源文件失败", e);
            return new Resource[0];
        }
    }

    /**
     * 读取资源文件内容
     */
    private String readResourceFile(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return readResourceContent(resource);
        } catch (Exception e) {
            log.error("读取资源文件失败: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * 读取资源内容
     */
    private String readResourceContent(Resource resource) {
        if (resource == null || !resource.exists()) {
            return null;
        }

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            log.error("读取资源内容失败", e);
            return null;
        }
    }
}