package com.itibari.auth.service.impl;

import com.itibari.auth.exception.BadRequestException;
import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.request.ChangePasswordRequest;
import com.itibari.auth.model.request.ResetPasswordRequest;
import com.itibari.auth.model.response.ResetPasswordTokenResponse;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.service.AuthUserLookupService;
import com.itibari.auth.service.NotificationClient;
import com.itibari.auth.service.ResetService;
import com.itibari.auth.service.ResetTokenService;
import com.itibari.auth.service.components.email.EmailBodyCrafter;
import com.itibari.auth.util.date.FormatDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResetServiceImpl implements ResetService {

    private static final int RESET_TOKEN_EXPIRY_MINUTES = 5;

    private final AuthUserRepository authUserRepository;
    private final AuthUserLookupService authUserLookupService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationClient notificationClient;
    private final ResetTokenService resetTokenService;

    @Transactional
    @Override
    public ResetPasswordTokenResponse requestPasswordChange(String identifier) {
        AuthUser user = authUserLookupService.findByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        String resetToken = resetTokenService.generateUniqueToken();
        LocalDateTime expiresAt = FormatDate.currentNairobiDateTime()
                .plusMinutes(RESET_TOKEN_EXPIRY_MINUTES);

        user.setForcePasswordReset(true);
        user.setResetPasswordTokenHash(passwordEncoder.encode(resetToken));
        user.setResetPasswordTokenLookupHash(resetTokenService.lookupHash(resetToken));
        user.setResetPasswordTokenExpiresAt(expiresAt);
        authUserRepository.save(user);

        ResetPasswordTokenResponse response = new ResetPasswordTokenResponse(
                resetToken, String.valueOf(RESET_TOKEN_EXPIRY_MINUTES));
        notificationClient.sendEmail(
                user.getEmail(),
                user.fullName(),
                "Password Reset",
                EmailBodyCrafter.resetPasswordEmailBody(response),
                tenantPublicId(user),
                user.getPublicId() == null ? null : user.getPublicId().toString());
        return response;
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        AuthUser user = authUserRepository.findByResetPasswordTokenLookupHashAndDeletedFalse(
                        resetTokenService.lookupHash(request.resetToken()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Password do not match");
        }

        if (!user.isForcePasswordReset()) {
            throw new BadRequestException("Password reset is not required for this user");
        }

        if (user.getResetPasswordTokenHash() == null
                || user.getResetPasswordTokenExpiresAt() == null
                || user.getResetPasswordTokenExpiresAt().isBefore(FormatDate.currentNairobiDateTime())
                || !passwordEncoder.matches(request.resetToken(), user.getResetPasswordTokenHash())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        applyNewPassword(user, request.newPassword());
    }

    @Transactional
    @Override
    public void changePassword(UUID userPublicId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Password do not match");
        }

        AuthUser user = authUserRepository.findByPublicIdAndDeletedIsFalse(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        applyNewPassword(user, request.newPassword());
    }

    private void applyNewPassword(AuthUser user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForcePasswordReset(false);
        user.setResetPasswordTokenHash(null);
        user.setResetPasswordTokenLookupHash(null);
        user.setResetPasswordTokenExpiresAt(null);
        authUserRepository.save(user);
    }

    private UUID tenantPublicId(AuthUser user) {
        if (user.getInstitution() != null) {
            return user.getInstitution().getPublicId();
        }
        if (user.getInstitutions() == null || user.getInstitutions().isEmpty()) {
            return null;
        }
        return user.getInstitutions().iterator().next().getPublicId();
    }
}
