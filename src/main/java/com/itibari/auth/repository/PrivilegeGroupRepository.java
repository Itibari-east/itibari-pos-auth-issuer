package com.itibari.auth.repository;

import com.itibari.auth.model.PrivilegeGroup;
import com.itibari.auth.model.response.PrivilegeGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PrivilegeGroupRepository extends JpaRepository<PrivilegeGroup, Long> {

    Optional<PrivilegeGroup> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<PrivilegeGroup> findByNameIgnoreCaseAndParentTenantPublicIdIsNullAndDeletedIsFalse(String name);

    Optional<PrivilegeGroup> findByNameIgnoreCaseAndParentTenantPublicIdAndDeletedIsFalse(
            String name, UUID parentTenantPublicId);

    @Query("""
            select new com.itibari.auth.model.response.PrivilegeGroupResponse(g.publicId, g.name)
            from PrivilegeGroup g
            where g.deleted = false
              and (
                   :parentTenantPublicId is null
                or g.parentTenantPublicId is null
                or g.parentTenantPublicId = :parentTenantPublicId
              )
              and (
                   :searchParam is null
                or :searchParam = ''
                or lower(g.name) like lower(concat('%', :searchParam, '%'))
              )
            order by g.name asc
            """)
    Page<PrivilegeGroupResponse> listGroups(@Param("searchParam") String searchParam,
                                            @Param("parentTenantPublicId") UUID parentTenantPublicId,
                                            Pageable pageable);

    @Query("""
            select new com.itibari.auth.model.response.PrivilegeGroupResponse(g.publicId, g.name)
            from Role r
            join r.privilegeGroupList g
            where r.deleted = false
              and r.publicId = :rolePublicId
              and g.deleted = false
            order by g.name asc
            """)
    Page<PrivilegeGroupResponse> listGroupsByRolePublicId(@Param("rolePublicId") UUID rolePublicId,
                                                          Pageable pageable);
}
