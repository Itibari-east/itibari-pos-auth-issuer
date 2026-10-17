package com.itibari.auth.repository;

import com.itibari.auth.model.Country;
import com.itibari.auth.repository.customized.CountryRepositoryCustomized;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, Long>, CountryRepositoryCustomized {

    Optional<Country> findByIsoAlphaThreeIgnoreCaseAndDeletedIsFalse(String isoAlphaThree);

    Optional<Country> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
