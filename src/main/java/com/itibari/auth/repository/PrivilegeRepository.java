package com.itibari.auth.repository;

import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.response.PrivilegeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Optional<Privilege> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<Privilege> findByNameIgnoreCaseAndDeletedIsFalse(String name);

    @Query("""
            select new com.itibari.auth.model.response.PrivilegeResponse(p.publicId, p.name)
            from Privilege p
            where p.deleted = false
              and (
                   :searchParam is null
                or :searchParam = ''
                or lower(p.name) like lower(concat('%', :searchParam, '%'))
              )
            order by p.name asc
            """)
    Page<PrivilegeResponse> listPrivileges(@Param("searchParam") String searchParam, Pageable pageable);

    @Query("""
            select new com.itibari.auth.model.response.PrivilegeResponse(p.publicId, p.name)
            from Privilege p
            where p.deleted = false
              and p.group.deleted = false
              and p.group.publicId = :groupPublicId
            order by p.name asc
            """)
    Page<PrivilegeResponse> listPrivilegesByGroupPublicId(@Param("groupPublicId") UUID groupPublicId,
                                                          Pageable pageable);
}
