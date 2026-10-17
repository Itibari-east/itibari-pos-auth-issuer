package com.itibari.auth.exception;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException() {
        super("Invalid credentials");
    }
}
