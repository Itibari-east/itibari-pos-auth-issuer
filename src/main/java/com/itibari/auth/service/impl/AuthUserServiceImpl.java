package com.itibari.auth.service.impl;

import com.itibari.auth.exception.BadRequestException;
import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.Institution;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.Role;
import com.itibari.auth.model.request.AuthUserRequest;
import com.itibari.auth.model.request.CreateUserDTO;
import com.itibari.auth.model.request.ShopAllocationDTO;
import com.itibari.auth.model.request.UserRoleDto;
import com.itibari.auth.model.response.AuthUserResponse;
import com.itibari.auth.model.response.RoleResponse;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.repository.InstitutionRepository;
import com.itibari.auth.repository.RoleRepository;
import com.itibari.auth.service.AuthUserService;
import com.itibari.auth.util.phone.FormatPhone;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthUserServiceImpl implements AuthUserService {

    private final AuthUserRepository authUserRepository;
    private final RoleRepository roleRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthUser createUser(CreateUserDTO dto) {
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }
        String email = dto.contactEmail().trim().toLowerCase();
        String phone = FormatPhone.myPhoneNumber(dto.contactPhoneNumber());

        if (authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(email).isPresent()) {
            throw new BadRequestException("Email already taken");
        }
        if (authUserRepository.findByPhoneNumberAndDeletedFalse(phone).isPresent()) {
            throw new BadRequestException("Phone number already taken");
        }
        if (authUserRepository.findByUsernameIgnoreCaseAndDeletedFalse(email).isPresent()) {
            throw new BadRequestException("Username already taken");
        }

        AuthUser user = AuthUser.builder()
                .username(email)
                .email(email)
                .phoneNumber(phone)
                .firstName(dto.firstName().trim())
                .lastName(dto.lastName().trim())
                .password(passwordEncoder.encode(dto.password()))
                .enabled(true)
                .activeStatus(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .build();

        return authUserRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getUserByPublicId(UUID publicId) {
        return toResponse(requireUser(publicId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuthUserResponse> getUsers(String searchParam, Pageable pageable) {
        return authUserRepository.listUsers(searchParam, pageable);
    }

    @Override
    @Transactional
    public AuthUserResponse updateUser(UUID userId, AuthUserRequest request) {
        AuthUser user = requireUser(userId);
        String email = request.email().trim().toLowerCase();
        String phone = FormatPhone.myPhoneNumber(request.phoneNumber());

        authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(email)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("Email already taken");
                });
        authUserRepository.findByPhoneNumberAndDeletedFalse(phone)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("Phone number already taken");
                });

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setUsername(email);
        return toResponse(authUserRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        AuthUser user = requireUser(userId);
        user.setDeleted(true);
        user.setEnabled(false);
        user.setActiveStatus(false);
        authUserRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getSessionUser(UUID requestingUserId) {
        return toResponse(requireUser(requestingUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> getUserRoles(UUID userId, String searchParam, Pageable pageable) {
        AuthUser user = requireUser(userId);
        List<RoleResponse> roles = user.getRoles().stream()
                .filter(role -> searchParam == null || searchParam.isBlank()
                        || role.getName().toLowerCase().contains(searchParam.toLowerCase()))
                .map(Role::toRoleResponse)
                .toList();
        return toPage(roles, pageable);
    }

    @Override
    @Transactional
    public String assignRoles(UUID userId, UserRoleDto request) {
        AuthUser user = requireUser(userId);
        List<Role> roles = request.rolePublicIds().stream()
                .map(rolePublicId -> roleRepository.findByPublicIdAndDeletedIsFalse(rolePublicId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role")))
                .toList();
        user.getRoles().addAll(roles);
        authUserRepository.save(user);
        return roles.stream().map(Role::getName).collect(Collectors.joining(", "));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Map<String, Map<String, Object>>> getUserPrivileges(UUID userId, Pageable pageable) {
        AuthUser user = requireUser(userId);
        List<Map<String, Map<String, Object>>> rolePrivileges = user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(role -> {
                    Set<Long> grantedPrivilegeIds = role.getPrivilegeList().stream()
                            .map(Privilege::getId)
                            .collect(Collectors.toSet());
                    Map<String, Object> roleDetails = new LinkedHashMap<>();
                    roleDetails.put("publicId", role.getPublicId());

                    role.getPrivilegeGroupList().stream()
                            .sorted(Comparator.comparing(group -> group.getName(),
                                    Comparator.nullsLast(String::compareToIgnoreCase)))
                            .forEach(group -> {
                                List<Map<String, Object>> privileges = group.getPrivileges().stream()
                                        .sorted(Comparator.comparing(Privilege::getName,
                                                Comparator.nullsLast(String::compareToIgnoreCase)))
                                        .map(privilege -> {
                                            Map<String, Object> privilegeDetails = new LinkedHashMap<>();
                                            privilegeDetails.put("publicId", privilege.getPublicId());
                                            privilegeDetails.put(privilege.getName(),
                                                    grantedPrivilegeIds.contains(privilege.getId()));
                                            return privilegeDetails;
                                        })
                                        .toList();
                                roleDetails.put(group.getName(), privileges);
                            });
                    return Map.<String, Map<String, Object>>of(role.getName(), roleDetails);
                })
                .toList();
        return toPage(rolePrivileges, pageable);
    }

    @Override
    @Transactional
    public AuthUser allocateShops(UUID requestingUserId, UUID userId, ShopAllocationDTO shopAllocationDTO) {
        Institution parentTenant = getParentTenantForUser(requestingUserId);
        UUID parentTenantId = parentTenant.getPublicId();
        AuthUser user = requireUser(userId);

        Set<Institution> allocatedShops = shopAllocationDTO.shopPublicIds().stream()
                .map(shopId -> institutionRepository.findByPublicIdAndDeletedIsFalse(shopId)
                        .orElseThrow(() -> new ResourceNotFoundException("Shop")))
                .peek(shop -> {
                    boolean isParent = parentTenantId.equals(shop.getPublicId());
                    boolean isChild = shop.getParent() != null
                            && parentTenantId.equals(shop.getParent().getPublicId());
                    if (!isParent && !isChild) {
                        throw new ResourceNotFoundException("Shop");
                    }
                })
                .collect(Collectors.toSet());

        user.getInstitutions().removeIf(shop -> shop.getParent() != null
                && parentTenantId.equals(shop.getParent().getPublicId()));
        user.getInstitutions().add(parentTenant);
        user.getInstitutions().addAll(allocatedShops);
        if (user.getInstitution() == null || allocatedShops.stream()
                .noneMatch(shop -> shop.getId().equals(user.getInstitution().getId()))) {
            user.setInstitution(allocatedShops.iterator().next());
        }
        return authUserRepository.save(user);
    }

    @Override
    @Transactional
    public AuthUser removeShopAllocation(UUID requestingUserId, UUID userId, UUID parentTenantId, UUID shopId) {
        Institution parentTenant = institutionRepository.findByPublicIdAndDeletedIsFalse(parentTenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent tenant"));
        if (!parentTenant.isParentTenant()) {
            throw new ResourceNotFoundException("Parent tenant");
        }
        if (!authUserRepository.belongsToInstitution(requestingUserId, parentTenantId)) {
            throw new AccessDeniedException("You do not belong to this parent tenant");
        }

        Institution shop = institutionRepository.findByPublicIdAndDeletedIsFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop"));
        boolean isParent = parentTenantId.equals(shop.getPublicId());
        boolean isChild = shop.getParent() != null && parentTenantId.equals(shop.getParent().getPublicId());
        if (!isParent && !isChild) {
            throw new ResourceNotFoundException("Shop");
        }
        if (isParent) {
            throw new AccessDeniedException("Parent tenant allocation cannot be removed");
        }

        AuthUser user = requireUser(userId);
        user.getInstitutions().removeIf(allocatedShop -> shopId.equals(allocatedShop.getPublicId()));
        if (user.getInstitution() != null && shopId.equals(user.getInstitution().getPublicId())) {
            user.setInstitution(user.getInstitutions().stream()
                    .filter(allocatedShop -> !allocatedShop.isDeleted())
                    .findFirst()
                    .orElse(null));
        }
        return authUserRepository.save(user);
    }

    private Institution getParentTenantForUser(UUID userPublicId) {
        AuthUser user = requireUser(userPublicId);
        return user.getInstitutions().stream()
                .filter(shop -> !shop.isDeleted() && shop.isParentTenant())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Parent shop"));
    }

    private AuthUser requireUser(UUID publicId) {
        return authUserRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private AuthUserResponse toResponse(AuthUser user) {
        return AuthUserResponse.builder()
                .publicId(user.getPublicId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .activeStatus(user.isActiveStatus())
                .build();
    }

    private static <T> Page<T> toPage(List<T> items, Pageable pageable) {
        int start = Math.toIntExact(Math.min(pageable.getOffset(), items.size()));
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }
}
