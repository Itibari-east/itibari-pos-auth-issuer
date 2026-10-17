package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotEmpty;

public record CreateChildShopDTO(
        @NotEmpty(message = "name is required") String name,
        String country,
        String traPin,
        String timeZone,
        String baseCurrency,
        String city
) {
}
