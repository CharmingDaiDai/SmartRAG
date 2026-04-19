package com.mtmn.smartrag.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * REST API 日志切面
 * 用于记录所有 REST API 的请求与响应信息
 *
 * @author charmingdaidai
 */
@Aspect
@Component
@Slf4j
public class RestApiLogAspect {

    /**
     * 定义切点：所有 controller 包下的方法
     */
    @Pointcut("execution(* com.mtmn.smartrag.controller.*.*(..))")
    public void apiPointcut() {
    }

    /**
     * 环绕通知：记录请求开始、结束时间以及请求参数和响应结果
     */
    @Around("apiPointcut()")
    public Object logAroundApi(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 记录HTTP方法、URL和IP地址
            log.info("🌐 请求开始 - 方法: {}, URL: {}, IP: {}, 类方法: {}.{}",
                    request.getMethod(),
                    request.getRequestURL().toString(),
                    request.getRemoteAddr(),
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());

            // 记录请求参数，但不记录敏感信息
            String params = Arrays.toString(joinPoint.getArgs());
            if (params.contains("password")) {
                params = "包含敏感信息，不记录详细参数";
            }
            log.debug("📝 请求参数: {}", params);
        }

        // 执行方法，获取返回值
        Object result = joinPoint.proceed();

        // 计算执行时间
        long executionTime = System.currentTimeMillis() - startTime;
        log.info("✅ 请求结束 - 执行时间: {}ms", executionTime);

        return result;
    }

    /**
     * 异常通知：记录方法抛出的异常
     */
    @AfterThrowing(pointcut = "apiPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("💥 API异常 - 类方法: {}.{}, 异常信息: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                e.getMessage());
    }
}