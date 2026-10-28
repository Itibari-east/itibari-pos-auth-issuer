package com.itibari.auth.service;

import com.itibari.auth.model.response.CurrencyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

@Service
public class CurrencyService {

    public Page<CurrencyResponse> getCurrencies(String searchParam, Pageable pageable) {
        List<CurrencyResponse> hits = new ArrayList<>();
        String query = searchParam == null ? "" : searchParam.trim().toLowerCase(Locale.ROOT);
        for (Currency currency : Currency.getAvailableCurrencies()) {
            String name = currency.getDisplayName(Locale.ENGLISH);
            if (!query.isEmpty()
                    && !currency.getCurrencyCode().toLowerCase(Locale.ROOT).contains(query)
                    && !name.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            hits.add(new CurrencyResponse(currency.getCurrencyCode(), name, currency.getDefaultFractionDigits()));
        }
        hits.sort(Comparator.comparing(CurrencyResponse::code));
        int from = (int) pageable.getOffset();
        if (from >= hits.size()) {
            return new PageImpl<>(List.of(), pageable, hits.size());
        }
        int to = Math.min(from + pageable.getPageSize(), hits.size());
        return new PageImpl<>(hits.subList(from, to), pageable, hits.size());
    }
}
