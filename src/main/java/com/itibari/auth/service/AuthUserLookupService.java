package com.itibari.auth.service;

import com.itibari.auth.model.AuthUser;

import java.util.Optional;

public interface AuthUserLookupService {
    Optional<AuthUser> findByIdentifier(String identifier);
}
