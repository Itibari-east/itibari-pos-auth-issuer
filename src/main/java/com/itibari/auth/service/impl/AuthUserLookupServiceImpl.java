package com.itibari.auth.service.impl;

import com.itibari.auth.model.AuthUser;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.service.AuthUserLookupService;
import com.itibari.auth.util.phone.FormatPhone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthUserLookupServiceImpl implements AuthUserLookupService {

    private final AuthUserRepository authUserRepository;

    @Override
    public Optional<AuthUser> findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();
        String normalizedPhone = FormatPhone.looksLikeEmail(trimmed)
                ? trimmed
                : FormatPhone.myPhoneNumber(trimmed);
        return authUserRepository.findByIdentifier(trimmed, normalizedPhone);
    }
}
