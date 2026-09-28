

package com.bitselect.agent.framework.web;

import com.bitselect.agent.framework.convention.Result;
import com.bitselect.agent.framework.errorcode.BaseErrorCode;
import com.bitselect.agent.framework.exception.ClientException;
import com.bitselect.agent.framework.exception.RemoteException;
import com.bitselect.agent.framework.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * 所有 Controller 抛出的异常在这里统一转换为 Result
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientException.class)
    public Result<Void> handleClientException(ClientException e, HttpServletRequest request) {
        log.warn("[客户端异常] uri={}, code={}, message={}", request.getRequestURI(), e.errorCode, e.errorMessage);
        return Results.failure(e.errorCode, e.errorMessage);
    }

    @ExceptionHandler(ServiceException.class)
    public Result<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("[服务端异常] uri={}, code={}, message={}", request.getRequestURI(), e.errorCode, e.errorMessage);
        return Results.failure(e.errorCode, e.errorMessage);
    }

    @ExceptionHandler(RemoteException.class)
    public Result<Void> handleRemoteException(RemoteException e, HttpServletRequest request) {
        log.warn("[远程调用异常] uri={}, code={}, message={}", request.getRequestURI(), e.errorCode, e.errorMessage);
        return Results.failure(e.errorCode, e.errorMessage);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(BaseErrorCode.CLIENT_ERROR.message());
        log.warn("[参数校验] uri={}, message={}", request.getRequestURI(), message);
        return Results.failure(BaseErrorCode.CLIENT_ERROR.code(), message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] uri={}", request.getRequestURI(), e);
        return Results.failure();                              // ← 无参
    }
}