package com.mkappworks.notification.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
    public ResourceNotFoundException(String res, String field, Object val) {
        super(String.format("%s not found with %s: '%s'", res, field, val));
    }
}
