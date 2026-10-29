package com.itibari.auth.util.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.itibari.auth.exception.BadRequestException;

public final class InternationalPhone {

    private static final PhoneNumberUtil PHONE_NUMBERS = PhoneNumberUtil.getInstance();

    private InternationalPhone() {
    }

    public static String toE164(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("phone is required");
        }
        try {
            Phonenumber.PhoneNumber parsed = PHONE_NUMBERS.parse(raw.trim(), null);
            if (!PHONE_NUMBERS.isPossibleNumber(parsed)) {
                throw new BadRequestException("Invalid phone number");
            }
            return PHONE_NUMBERS.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ex) {
            throw new BadRequestException("Invalid phone number");
        }
    }
}
