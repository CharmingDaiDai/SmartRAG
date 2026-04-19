package com.mtmn.smartrag.common;

import com.mtmn.smartrag.exception.ConfigValidationException;
import com.mtmn.smartrag.exception.MilvusConnectionException;
import com.mtmn.smartrag.exception.ResourceNotFoundException;
import com.mtmn.smartrag.exception.UnauthorizedAccessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author charmingdaidai
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        String message = String.join(", ", errors);
        log.warn("参数验证失败: {}", message);
        return ApiResponse.badRequest(message);
    }

    /**
     * 处理绑定异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ApiResponse<String> handleBindException(BindException e) {
        List<String> errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        String message = String.join(", ", errors);
        log.warn("参数绑定失败: {}", message);
        return ApiResponse.badRequest(message);
    }

    /**
     * 处理约束违反异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<String> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束违反: {}", e.getMessage());
        return ApiResponse.badRequest(e.getMessage());
    }

    /**
     * 处理请求参数缺失异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("请求参数缺失: {}", e.getMessage());
        return ApiResponse.badRequest("缺少必要的请求参数: " + e.getParameterName());
    }

    /**
     * 处理方法参数类型不匹配异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("方法参数类型不匹配: {}", e.getMessage());
        return ApiResponse.badRequest("参数类型不匹配: " + e.getName());
    }

    /**
     * 处理HTTP消息不可读异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HTTP消息不可读: {}", e.getMessage());
        return ApiResponse.badRequest("请求体格式错误或为空");
    }

    /**
     * 处理请求方法不支持异常
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ApiResponse.error(405, "不支持的请求方法: " + e.getMethod());
    }

    /**
     * 处理404异常（Spring 6.0+）
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<String> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("静态资源未找到: {}", e.getResourcePath());
        return ApiResponse.notFound("请求的资源不存在");
    }

    /**
     * 处理资源未找到异常（Spring 5.x及以下）
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("处理器未找到: {}", e.getMessage());
        return ApiResponse.notFound("请求的资源不存在: " + e.getRequestURL());
    }

    /**
     * 处理业务资源未找到异常
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ApiResponse<String> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("业务资源未找到: {}", e.getMessage());
        return ApiResponse.notFound(e.getMessage());
    }

    /**
     * 处理未授权访问异常
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ApiResponse<String> handleUnauthorizedAccessException(UnauthorizedAccessException e) {
        log.warn("未授权访问: {}", e.getMessage());
        return ApiResponse.forbidden(e.getMessage());
    }

    /**
     * 处理配置验证异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConfigValidationException.class)
    public ApiResponse<String> handleConfigValidationException(ConfigValidationException e) {
        log.warn("配置验证失败: {}", e.getMessage());
        return ApiResponse.badRequest(e.getMessage());
    }

    /**
     * 处理认证异常
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<String> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return ApiResponse.unauthorized("认证失败: " + e.getMessage());
    }

    /**
     * 处理Bad Credentials异常
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException.class)
    public ApiResponse<String> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("凭证错误: {}", e.getMessage());
        return ApiResponse.unauthorized("用户名或密码错误");
    }

    /**
     * 处理访问拒绝异常
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<String> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return ApiResponse.forbidden("没有权限访问此资源");
    }

    /**
     * 处理文件上传大小超出限制异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超出限制: {}", e.getMessage());
        return ApiResponse.badRequest("上传文件大小超出限制");
    }

    /**
     * 处理Multipart异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MultipartException.class)
    public ApiResponse<String> handleMultipartException(MultipartException e) {
        log.warn("Multipart请求错误: {}", e.getMessage());
        return ApiResponse.badRequest("文件上传请求格式错误，请确保请求类型为multipart/form-data");
    }

    /**
     * 处理 Milvus 连接异常
     */
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(MilvusConnectionException.class)
    public ApiResponse<String> handleMilvusConnectionException(MilvusConnectionException e) {
        log.error("Milvus 服务异常: {}", e.getMessage());
        return ApiResponse.error(503, "向量数据库服务暂时不可用，请联系管理员或稍后重试");
    }

    /**
     * 处理自定义异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CustomException.class)
    public ApiResponse<String> handleCustomException(CustomException e) {
        log.warn("自定义异常: {}", e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理其他未捕获的异常
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception e) {
        // 记录完整堆栈信息，但只返回简化的错误消息给客户端
        log.error("未捕获的异常 [{}]: {}", e.getClass().getName(), e.getMessage(), e);
        
        // 生产环境不暴露详细错误信息
        String message = isProductionEnvironment() 
            ? "服务器内部错误，请联系管理员" 
            : "服务器内部错误：" + e.getMessage();
            
        return ApiResponse.error(message);
    }

    /**
     * 判断是否为生产环境
     * TODO: 从配置中读取环境变量
     */
    private boolean isProductionEnvironment() {
        // 可以通过 @Value("${spring.profiles.active}") 注入
        return false; // 开发环境暂时返回详细信息
    }
}