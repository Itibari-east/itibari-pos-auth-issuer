package com.itibari.auth.service;

import com.itibari.auth.model.request.ResetPasswordRequest;
import com.itibari.auth.model.response.ResetPasswordTokenResponse;

public interface ResetService {

    ResetPasswordTokenResponse requestPasswordChange(String identifier);

    void resetPassword(ResetPasswordRequest request);
}
