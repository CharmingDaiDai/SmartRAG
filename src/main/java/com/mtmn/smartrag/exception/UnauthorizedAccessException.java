package com.mtmn.smartrag.exception;

/**
 * 未授权访问异常
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(Long userId, String resourceType, Long resourceId) {
        super(String.format("User %d is not authorized to access %s %d", userId, resourceType, resourceId));
    }
}