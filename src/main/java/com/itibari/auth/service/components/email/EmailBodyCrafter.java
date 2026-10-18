package com.itibari.auth.service.components.email;

public final class EmailBodyCrafter {

    private EmailBodyCrafter() {
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
