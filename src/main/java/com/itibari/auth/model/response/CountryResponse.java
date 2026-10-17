package com.itibari.auth.model.response;

import java.util.UUID;

public record CountryResponse(UUID publicId, String name, String isoAlpha, String countryCode) {
}
