package com.mtmn.smartrag.exception;

/**
 * 配置验证异常
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public class ConfigValidationException extends RuntimeException {

    public ConfigValidationException(String message) {
        super(message);
    }

    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}