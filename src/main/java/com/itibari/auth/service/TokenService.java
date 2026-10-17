package com.itibari.auth.service;

import com.itibari.auth.dataTypes.LoginChannel;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.TokenResponse;

import java.util.UUID;

public interface TokenService {
    TokenResponse generateToken(AuthUser authUser);

    TokenResponse generateGoogleToken(AuthUser authUser);

    TokenResponse refreshToken(UUID userPublicId, LoginChannel channel);

    TokenResponse switchShop(UUID userPublicId, UUID institutionPublicId, LoginChannel channel);

    TokenResponse parentShopCreatedToken(UUID userPublicId, UUID parentShopPublicId, LoginChannel channel);
}
