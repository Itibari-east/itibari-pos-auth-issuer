package com.itibari.auth.repository;

import com.itibari.auth.model.Institution;
import com.itibari.auth.model.response.ProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<Institution> findByDeletedIsFalseAndMainTenantIsTrue();

    @Query("""
            select i from Institution i
            where i.deleted = false
              and i.parentTenant = true
              and (
                   :searchParam is null
                or :searchParam = ''
                or lower(i.name) like lower(concat('%', :searchParam, '%'))
                or lower(i.identityNumber) like lower(concat('%', :searchParam, '%'))
              )
            order by i.creationTime desc
            """)
    Page<Institution> searchParentTenants(@Param("searchParam") String searchParam, Pageable pageable);
}
