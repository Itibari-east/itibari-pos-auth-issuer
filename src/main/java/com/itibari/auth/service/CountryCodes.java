package com.itibari.auth.service;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

public final class CountryCodes {

    private static final List<CatalogCountry> COUNTRIES = loadCountries();

    private CountryCodes() {
    }

    public static List<CatalogCountry> countries() {
        return COUNTRIES;
    }

    public static CatalogCountry byAlpha3(String isoAlphaThree) {
        if (blank(isoAlphaThree)) {
            return null;
        }
        String needle = isoAlphaThree.trim().toUpperCase(Locale.ROOT);
        for (CatalogCountry country : COUNTRIES) {
            if (country.isoAlphaThree().equals(needle)) {
                return country;
            }
        }
        return null;
    }

    public static String alphaTwo(String isoAlphaThree) {
        CatalogCountry country = byAlpha3(isoAlphaThree);
        return country == null ? null : country.isoAlphaTwo();
    }

    public static String currency(String isoAlphaTwo) {
        if (blank(isoAlphaTwo)) {
            return null;
        }
        try {
            Currency currency = Currency.getInstance(Locale.of("", isoAlphaTwo.trim().toUpperCase(Locale.ROOT)));
            if (currency == null) {
                return null;
            }
            return currency.getCurrencyCode();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static List<CatalogCountry> search(String searchParam) {
        String query = searchParam.trim();
        String lower = query.toLowerCase(Locale.ROOT);
        String queryDigits = digits(query);
        List<CatalogCountry> hits = new ArrayList<>();
        for (CatalogCountry country : COUNTRIES) {
            if (matches(country, lower, queryDigits)) {
                hits.add(country);
            }
        }
        return hits;
    }

    private static boolean matches(CatalogCountry country, String lower, String queryDigits) {
        if (country.name().toLowerCase(Locale.ROOT).contains(lower)
                || country.isoAlphaTwo().toLowerCase(Locale.ROOT).contains(lower)
                || country.isoAlphaThree().toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        if (country.dial() == null || queryDigits.isEmpty() || !isDigits(queryDigits)) {
            return false;
        }
        return digits(country.dial()).equals(queryDigits);
    }

    private static List<CatalogCountry> loadCountries() {
        PhoneNumberUtil phones = PhoneNumberUtil.getInstance();
        List<CatalogCountry> countries = new ArrayList<>();
        for (String alphaTwo : Locale.getISOCountries()) {
            Locale locale = Locale.of("", alphaTwo);
            String alphaThree;
            try {
                alphaThree = locale.getISO3Country();
            } catch (MissingResourceException ex) {
                continue;
            }
            if (blank(alphaThree)) {
                continue;
            }
            int callingCode = phones.getCountryCodeForRegion(alphaTwo);
            String dial = callingCode > 0 ? "+" + callingCode : null;
            countries.add(new CatalogCountry(
                    locale.getDisplayCountry(Locale.ENGLISH),
                    alphaTwo,
                    alphaThree.toUpperCase(Locale.ROOT),
                    dial,
                    currency(alphaTwo)));
        }
        return List.copyOf(countries);
    }

    private static String digits(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static boolean isDigits(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') {
                return false;
            }
        }
        return !value.isEmpty();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CatalogCountry(
            String name,
            String isoAlphaTwo,
            String isoAlphaThree,
            String dial,
            String currencyCode
    ) {
    }
}
