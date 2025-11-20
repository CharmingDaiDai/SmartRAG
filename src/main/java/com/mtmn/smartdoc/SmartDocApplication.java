package com.mtmn.smartdoc;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author charmingdaidai
 */
@SpringBootApplication
public class SmartDocApplication {

    public static void main(String[] args) {
        // 加载 .env 文件中的环境变量
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            // 将 .env 文件中的变量设置为系统属性
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
        } catch (Exception e) {
            System.err.println("Warning: Failed to load .env file: " + e.getMessage());
        }

        SpringApplication.run(SmartDocApplication.class, args);
    }

}