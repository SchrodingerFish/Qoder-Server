package com.example.sqlexecutor.exception;

/**
 * 无效SQL异常
 */
public class InvalidSqlException extends RuntimeException {

    public InvalidSqlException(String message) {
        super(message);
    }

    public InvalidSqlException(String message, Throwable cause) {
        super(message, cause);
    }
}