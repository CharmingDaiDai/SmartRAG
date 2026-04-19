package com.mtmn.smartrag.common;

/**
 * 自定义异常类，用于处理业务逻辑中的特定异常。
 */
public class CustomException extends RuntimeException {

    private final int code;

    /**
     * 构造方法，使用默认错误码 400。
     *
     * @param message 错误信息
     */
    public CustomException(String message) {
        super(message);
        this.code = 400; // 默认错误码
    }

    /**
     * 构造方法，允许指定错误码。
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public CustomException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}
