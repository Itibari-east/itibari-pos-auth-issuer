package com.itibari.auth.service;

public interface ResetTokenService {

    String generateUniqueLinkToken();

    String lookupHash(String resetToken);
}
