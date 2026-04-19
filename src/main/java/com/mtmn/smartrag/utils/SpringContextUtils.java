package com.mtmn.smartrag.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description SpringContext 工具类
 * @date 2025/5/30 17:23
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {
    private static ApplicationContext context;

    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringContextUtils.context = applicationContext;
    }
}