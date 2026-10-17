package com.itibari.auth.util.date;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class FormatDate {
    private static final ZoneId NAIROBI_ZONE = ZoneId.of("Africa/Nairobi");

    private FormatDate() {
    }

    public static LocalDateTime currentNairobiDateTime() {
        return LocalDateTime.ofInstant(Instant.now(), NAIROBI_ZONE);
    }
}
