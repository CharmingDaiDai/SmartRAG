package com.mtmn.smartrag.rag.support;

import com.mtmn.smartrag.exception.MilvusConnectionException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * RAG 流式错误映射器。
 * 负责将底层异常映射为前端可展示的脱敏文案。
 */
public final class RagStreamErrorMapper {

    private RagStreamErrorMapper() {
    }

    public static String toUserMessage(Throwable error) {
        Throwable root = unwrap(error);
        if (root == null) {
            return "服务异常，请稍后重试。";
        }

        if (root instanceof MilvusConnectionException) {
            return "向量数据库服务暂时不可用，请稍后重试。";
        }

        if (isAuthError(root)) {
            return "模型认证失败，请联系管理员检查配置。";
        }

        if (isRateLimitError(root)) {
            return "AI 服务请求频率超限，请稍后再试。";
        }

        if (isNetworkError(root)) {
            return "网络连接异常，请稍后重试。";
        }

        return "服务异常，请稍后重试。";
    }

    public static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException || current instanceof ExecutionException) {
            if (current.getCause() == null) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }

    private static boolean isAuthError(Throwable error) {
        String className = error.getClass().getName();
        String msg = safeLower(error.getMessage());
        return className.endsWith("AuthenticationException")
                || msg.contains("unauthorized")
                || msg.contains("authentication failed")
                || msg.contains("invalid api key")
                || msg.contains("invalid token")
                || msg.contains("401");
    }

    private static boolean isRateLimitError(Throwable error) {
        String msg = safeLower(error.getMessage());
        return msg.contains("429")
                || msg.contains("rate limit")
                || msg.contains("too many requests")
                || msg.contains("速率限制")
                || msg.contains("请求频率");
    }

    private static boolean isNetworkError(Throwable error) {
        if (error instanceof SocketTimeoutException
                || error instanceof ConnectException
                || error instanceof IOException) {
            return true;
        }

        String msg = safeLower(error.getMessage());
        return msg.contains("timeout")
                || msg.contains("connection reset")
                || msg.contains("connection refused")
                || msg.contains("connection timed out");
    }

    private static String safeLower(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }
}