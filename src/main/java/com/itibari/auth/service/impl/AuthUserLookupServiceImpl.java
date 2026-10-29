package com.itibari.auth.service.impl;

import com.itibari.auth.exception.BadRequestException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.service.AuthUserLookupService;
import com.itibari.auth.util.phone.FormatPhone;
import com.itibari.auth.util.phone.InternationalPhone;
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
        String normalizedPhone;
        if (FormatPhone.looksLikeEmail(trimmed)) {
            normalizedPhone = trimmed;
        } else {
            try {
                normalizedPhone = InternationalPhone.toE164(trimmed);
            } catch (BadRequestException ex) {
                return Optional.empty();
            }
        }
        return authUserRepository.findByIdentifier(trimmed, normalizedPhone);
    }
}
