package com.itibari.auth.service;

public interface ResetTokenService {

    String generateUniqueToken();

    String generateUniqueLinkToken();

    String lookupHash(String resetToken);
}
