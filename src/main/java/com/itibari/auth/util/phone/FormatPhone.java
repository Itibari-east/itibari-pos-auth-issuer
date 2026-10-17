package com.itibari.auth.util.phone;

public final class FormatPhone {
    private FormatPhone() {
    }

    public static boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }

    public static String myPhoneNumber(String phoneNumber) {
        return String.format("+255%s", lastNineDigits(phoneNumber));
    }

    public static String lastNineDigits(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() <= 9) {
            return cleaned;
        }
        return cleaned.substring(cleaned.length() - 9);
    }
}
