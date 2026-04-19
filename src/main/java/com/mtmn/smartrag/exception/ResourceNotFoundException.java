package com.mtmn.smartrag.exception;

/**
 * 资源未找到异常
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s with id %d not found", resourceType, id));
    }
}