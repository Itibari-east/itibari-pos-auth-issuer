package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCountryRequest(@NotBlank String isoAlpha) {
}
