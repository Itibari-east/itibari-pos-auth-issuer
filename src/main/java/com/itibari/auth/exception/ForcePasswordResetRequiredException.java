package com.itibari.auth.exception;

public class ForcePasswordResetRequiredException extends RuntimeException {
    public ForcePasswordResetRequiredException() {
        super("Password reset is required before login");
    }
}
