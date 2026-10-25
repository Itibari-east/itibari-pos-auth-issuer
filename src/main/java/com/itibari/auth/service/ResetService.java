package com.itibari.auth.service;

import com.itibari.auth.model.request.ChangePasswordRequest;
import com.itibari.auth.model.request.ResetPasswordRequest;
import com.itibari.auth.model.response.ResetPasswordTokenResponse;

import java.util.UUID;

public interface ResetService {

    ResetPasswordTokenResponse requestPasswordChange(String identifier);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(UUID userPublicId, ChangePasswordRequest request);
}
