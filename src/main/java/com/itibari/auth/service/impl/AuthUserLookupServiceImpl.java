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
        if (FormatPhone.looksLikeEmail(trimmed)) {
            return authUserRepository.findByIdentifier(trimmed, trimmed);
        }
        Optional<AuthUser> match;
        try {
            match = authUserRepository.findByIdentifier(trimmed, InternationalPhone.toE164(trimmed));
        } catch (BadRequestException ex) {
            match = Optional.empty();
        }
        if (match.isPresent()) {
            return match;
        }
        String lastNine = FormatPhone.lastNineDigits(trimmed);
        if (lastNine == null || lastNine.length() < 9) {
            return Optional.empty();
        }
        return authUserRepository.findFirstByPhoneNumberEndingWithAndDeletedFalse(lastNine);
    }
}
