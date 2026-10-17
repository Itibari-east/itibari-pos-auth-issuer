package com.itibari.auth.model.request;

public record UpdateShopDTO(
        String name,
        String country,
        String traPin,
        String timeZone,
        String baseCurrency,
        String city
) {
}
