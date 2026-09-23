package com.ashokmart.service;

import com.ashokmart.model.AuthenticationResult;

public interface AuthService {
    AuthenticationResult register(String name, String email, String plaintextPassword);

    AuthenticationResult login(String email, String plaintextPassword);
}
