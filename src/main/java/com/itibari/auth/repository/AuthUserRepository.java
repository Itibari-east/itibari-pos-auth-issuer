package com.itibari.auth.repository;

import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.response.AuthUserResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByPublicIdAndDeletedIsFalse(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AuthUser u where u.publicId = :publicId and u.deleted = false")
    Optional<AuthUser> lockByPublicId(@Param("publicId") UUID publicId);

    Optional<AuthUser> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<AuthUser> findByUsernameIgnoreCaseAndDeletedFalse(String username);

    Optional<AuthUser> findByPhoneNumberAndDeletedFalse(String phoneNumber);

    Optional<AuthUser> findByGoogleSubjectAndDeletedFalse(String googleSubject);

    java.util.List<AuthUser> findByInstitutions_PublicIdAndDeletedFalse(UUID institutionPublicId);

    default boolean belongsToInstitution(UUID userPublicId, UUID institutionPublicId) {
        return findByPublicIdAndDeletedIsFalse(userPublicId)
                .map(user -> user.getInstitutions().stream()
                        .anyMatch(shop -> institutionPublicId.equals(shop.getPublicId()) && !shop.isDeleted()))
                .orElse(false);
    }

    @Query("""
            select u from AuthUser u
            where u.deleted = false
              and (
                   lower(u.username) = lower(:identifier)
                or lower(u.email) = lower(:identifier)
                or u.phoneNumber = :identifier
                or u.phoneNumber = :normalizedPhone
              )
            """)
    Optional<AuthUser> findByIdentifier(@Param("identifier") String identifier,
                                        @Param("normalizedPhone") String normalizedPhone);

    @Query("""
            select new com.itibari.auth.model.response.AuthUserResponse(
                u.publicId, u.firstName, u.lastName, u.phoneNumber, u.email, u.activeStatus)
            from AuthUser u
            where u.deleted = false
              and (
                   :searchParam is null
                or :searchParam = ''
                or lower(u.firstName) like lower(concat('%', :searchParam, '%'))
                or lower(u.lastName) like lower(concat('%', :searchParam, '%'))
                or lower(u.email) like lower(concat('%', :searchParam, '%'))
                or u.phoneNumber like concat('%', :searchParam, '%')
              )
            order by u.creationTime desc
            """)
    Page<AuthUserResponse> listUsers(@Param("searchParam") String searchParam, Pageable pageable);
}
