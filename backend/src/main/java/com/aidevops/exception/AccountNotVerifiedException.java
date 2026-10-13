package com.aidevops.exception;

import org.springframework.http.HttpStatus;

public class AccountNotVerifiedException extends ApiException {
    public AccountNotVerifiedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
