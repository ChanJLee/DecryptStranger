package com.theOldMen.exception;

/**
 * Created by jz on 2015/4/21.
 */
public class MyException extends Exception {
    private static final long serialVersionUID = 1L;

    public MyException(String message) {
        super(message);
    }

    public MyException(String message, Throwable cause) {
        super(message, cause);
    }
}
