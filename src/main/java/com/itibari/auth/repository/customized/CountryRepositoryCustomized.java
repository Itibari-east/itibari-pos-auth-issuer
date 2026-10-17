package com.itibari.auth.repository.customized;

import com.itibari.auth.model.response.CountryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CountryRepositoryCustomized {
    Page<CountryResponse> list(String searchParam, Pageable pageable);
}
