package com.itibari.auth.service;

import com.itibari.auth.model.AuthUser;
import org.springframework.security.oauth2.jwt.Jwt;

public interface GoogleLoginService {
    AuthUser findOrCreateUser(Jwt idToken);
}
