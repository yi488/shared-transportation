package com.example.shared_transportation.common;

import lombok.Getter;

/**
 * 业务异常：携带错误码，由全局异常处理器统一转换为 ApiResponse。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
