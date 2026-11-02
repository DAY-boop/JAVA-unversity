package com.demo.university.dao;

/**
 * 数据访问异常：把受检的 SQLException 统一成运行时异常，
 * 由 DefaultExceptionMapper 兜底成 500，避免每层都写 try-catch。
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
