package com.itibari.auth.repository;

import com.itibari.auth.model.Role;
import com.itibari.auth.model.response.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<Role> findByNameIgnoreCaseAndParentTenantPublicIdAndDeletedIsFalse(String name, UUID parentTenantPublicId);

    Optional<Role> findByNameIgnoreCaseAndParentTenantPublicIdIsNullAndDeletedIsFalse(String name);

    @Query("""
            select new com.itibari.auth.model.response.RoleResponse(r.publicId, r.name)
            from Role r
            where r.deleted = false
              and (:parentTenantPublicId is null or r.parentTenantPublicId = :parentTenantPublicId)
              and (
                   :searchParam is null
                or :searchParam = ''
                or lower(r.name) like lower(concat('%', :searchParam, '%'))
              )
            order by r.name asc
            """)
    Page<RoleResponse> listRoles(@Param("searchParam") String searchParam,
                                 @Param("parentTenantPublicId") UUID parentTenantPublicId,
                                 Pageable pageable);
}
