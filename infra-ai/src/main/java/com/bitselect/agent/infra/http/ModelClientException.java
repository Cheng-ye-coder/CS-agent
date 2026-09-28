

package com.bitselect.agent.infra.http;

import lombok.Getter;

/**
 * 模型客户端异常类
 */
@Getter
public class ModelClientException extends RuntimeException {

    private final ModelClientErrorType errorType;
    private final Integer statusCode;

    public ModelClientException(String message, ModelClientErrorType errorType, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }

    public ModelClientException(String message, ModelClientErrorType errorType, Integer statusCode) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }
}