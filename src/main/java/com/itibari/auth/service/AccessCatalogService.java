package com.itibari.auth.service;

import com.itibari.auth.model.AuthUser;

import java.util.UUID;

public interface AccessCatalogService {

    void seedGlobalCatalog();

    void seedTenantRoles(UUID tenantPublicId, AuthUser owner);
}
