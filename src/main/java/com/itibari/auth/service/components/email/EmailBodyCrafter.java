package com.itibari.auth.service.components.email;

import com.itibari.auth.model.response.ResetPasswordTokenResponse;

public final class EmailBodyCrafter {

    private EmailBodyCrafter() {
    }

    public static String resetPasswordEmailBody(ResetPasswordTokenResponse resetPasswordTokenResponse) {
        return String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.5;">
                        <p>Hello,</p>
                        <p>Your password reset token is
                            <strong style="font-size: 18px; color: #2E86C1;">%s</strong>.
                        </p>
                        <p>This code will expire in <strong>%s minutes</strong>.</p>
                        <p>Please enter this token on the reset page along with your new password.</p>
                        <br>
                        <p>Regards,<br>
                        The Support Team</p>
                    </body>
                </html>
                """, resetPasswordTokenResponse.resetToken(), resetPasswordTokenResponse.expiresAt());
    }

    public static String passwordSetupLinkEmailBody(String name, String setupLink, long expiryHours) {
        return String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.5;">
                        <p>Hello %s,</p>
                        <p>Your account has been created. Click the button below to set your password.</p>
                        <p>
                            <a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #2E86C1;
                               color: #ffffff; text-decoration: none; border-radius: 4px;">Set your password</a>
                        </p>
                        <p>If the button does not work, copy and paste this link into your browser:<br>%s</p>
                        <p>This link will expire in <strong>%d hours</strong> and can only be used once.</p>
                        <br>
                        <p>Regards,<br>
                        The Support Team</p>
                    </body>
                </html>
                """, name, setupLink, setupLink, expiryHours);
    }
}
