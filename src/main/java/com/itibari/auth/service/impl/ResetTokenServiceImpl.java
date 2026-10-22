package com.itibari.auth.service.impl;

import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.service.ResetTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ResetTokenServiceImpl implements ResetTokenService {

    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 10;
    private static final int LINK_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthUserRepository authUserRepository;

    @Override
    public String generateUniqueToken() {
        return generateUnique(() -> String.format("%06d", SECURE_RANDOM.nextInt(1_000_000)));
    }

    @Override
    public String generateUniqueLinkToken() {
        return generateUnique(() -> {
            byte[] bytes = new byte[LINK_TOKEN_BYTES];
            SECURE_RANDOM.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        });
    }

    private String generateUnique(Supplier<String> tokenSupplier) {
        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String resetToken = tokenSupplier.get();
            if (!authUserRepository.existsByResetPasswordTokenLookupHash(lookupHash(resetToken))) {
                return resetToken;
            }
        }
        throw new IllegalStateException("Unable to generate a unique reset token");
    }

    @Override
    public String lookupHash(String resetToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(resetToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
