package com.mtmn.smartdoc.common;

import com.mtmn.smartdoc.exception.ConfigValidationException;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.exception.UnauthorizedAccessException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 单元测试
 * 
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-21
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    void testHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("KnowledgeBase", 1L);

        // When
        ApiResponse<String> response = exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(404);
        assertThat(response.getMessage()).contains("KnowledgeBase");
        assertThat(response.getMessage()).contains("not found");
    }

    @Test
    void testHandleUnauthorizedAccessException() {
        // Given
        UnauthorizedAccessException exception = new UnauthorizedAccessException(1L, "KnowledgeBase", 2L);

        // When
        ApiResponse<String> response = exceptionHandler.handleUnauthorizedAccessException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(403);
        assertThat(response.getMessage()).contains("not authorized");
    }

    @Test
    void testHandleConfigValidationException() {
        // Given
        ConfigValidationException exception = new ConfigValidationException("Invalid embedding model");

        // When
        ApiResponse<String> response = exceptionHandler.handleConfigValidationException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("Invalid embedding model");
    }

    @Test
    void testHandleNoResourceFoundException() {
        // Given
        NoResourceFoundException exception = new NoResourceFoundException(null, "api/knowledge-bases");

        // When
        ApiResponse<String> response = exceptionHandler.handleNoResourceFoundException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(404);
        assertThat(response.getMessage()).contains("请求的资源不存在");
    }

    @Test
    void testHandleNoHandlerFoundException() throws NoHandlerFoundException {
        // Given
        NoHandlerFoundException exception = new NoHandlerFoundException(
                "GET", "/api/unknown", null);

        // When
        ApiResponse<String> response = exceptionHandler.handleNoHandlerFoundException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(404);
        assertThat(response.getMessage()).contains("请求的资源不存在");
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindException bindException = new BindException(new Object(), "test");
        bindException.addError(new FieldError("test", "field1", "字段验证失败"));
        
        when(exception.getBindingResult()).thenReturn(bindException.getBindingResult());

        // When
        ApiResponse<String> response = exceptionHandler.handleMethodArgumentNotValidException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("字段验证失败");
    }

    @Test
    void testHandleConstraintViolationException() {
        // Given
        ConstraintViolationException exception = new ConstraintViolationException("约束违反", new HashSet<>());

        // When
        ApiResponse<String> response = exceptionHandler.handleConstraintViolationException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("约束违反");
    }

    @Test
    void testHandleMissingServletRequestParameterException() {
        // Given
        MissingServletRequestParameterException exception = 
                new MissingServletRequestParameterException("userId", "Long");

        // When
        ApiResponse<String> response = exceptionHandler.handleMissingServletRequestParameterException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("userId");
    }

    @Test
    void testHandleHttpMessageNotReadableException() {
        // Given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");

        // When
        ApiResponse<String> response = exceptionHandler.handleHttpMessageNotReadableException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("请求体格式错误");
    }

    @Test
    void testHandleHttpRequestMethodNotSupportedException() {
        // Given
        HttpRequestMethodNotSupportedException exception = 
                new HttpRequestMethodNotSupportedException("POST");

        // When
        ApiResponse<String> response = exceptionHandler.handleHttpRequestMethodNotSupportedException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(405);
        assertThat(response.getMessage()).contains("POST");
    }

    @Test
    void testHandleBadCredentialsException() {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        // When
        ApiResponse<String> response = exceptionHandler.handleBadCredentialsException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(401);
        assertThat(response.getMessage()).contains("用户名或密码错误");
    }

    @Test
    void testHandleAccessDeniedException() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // When
        ApiResponse<String> response = exceptionHandler.handleAccessDeniedException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(403);
        assertThat(response.getMessage()).contains("没有权限");
    }

    @Test
    void testHandleMaxUploadSizeExceededException() {
        // Given
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(100L);

        // When
        ApiResponse<String> response = exceptionHandler.handleMaxUploadSizeExceededException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("文件大小超出限制");
    }

    @Test
    void testHandleCustomException() {
        // Given
        CustomException exception = new CustomException(400, "自定义错误");

        // When
        ApiResponse<String> response = exceptionHandler.handleCustomException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("自定义错误");
    }

    @Test
    void testHandleCustomException_WithDefaultCode() {
        // Given
        CustomException exception = new CustomException("默认错误码异常");

        // When
        ApiResponse<String> response = exceptionHandler.handleCustomException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("默认错误码异常");
    }

    @Test
    void testHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("未知异常");

        // When
        ApiResponse<String> response = exceptionHandler.handleException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).contains("服务器内部错误");
    }

    @Test
    void testHandleException_WithNullPointerException() {
        // Given
        NullPointerException exception = new NullPointerException("空指针异常");

        // When
        ApiResponse<String> response = exceptionHandler.handleException(exception);

        // Then
        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isNotEmpty();
    }
}
