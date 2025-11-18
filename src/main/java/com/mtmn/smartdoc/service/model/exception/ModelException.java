package com.mtmn.smartdoc.service.model.exception;

/**
 * 模型相关异常
 * 
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
public class ModelException extends RuntimeException {
    
    public ModelException(String message) {
        super(message);
    }
    
    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ModelException(Throwable cause) {
        super(cause);
    }
}
