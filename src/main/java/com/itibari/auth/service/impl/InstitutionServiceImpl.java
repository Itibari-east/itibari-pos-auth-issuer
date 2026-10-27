package com.itibari.auth.service.impl;

import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.mapper.InstitutionMapper;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.Institution;
import com.itibari.auth.model.NotificationPreference;
import com.itibari.auth.exception.BadRequestException;
import com.itibari.auth.model.request.CreateChildShopDTO;
import com.itibari.auth.model.request.CreateProfileDTO;
import com.itibari.auth.model.request.NotificationDTO;
import com.itibari.auth.model.request.PaymentDetail;
import com.itibari.auth.model.request.UpdateShopDTO;
import com.itibari.auth.model.response.ChildShopResponse;
import com.itibari.auth.model.response.NotificationPreferenceResponse;
import com.itibari.auth.model.response.ProfileResponse;
import com.itibari.auth.model.response.ShopSummaryResponse;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.repository.InstitutionRepository;
import com.itibari.auth.repository.NotificationPreferenceRepository;
import com.itibari.auth.repository.ShopSubscriptionDetailRepository;
import com.itibari.auth.service.AccessCatalogService;
import com.itibari.auth.service.InstitutionService;
import com.itibari.auth.service.SubscriptionAccessService;
import com.itibari.auth.util.date.FormatDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstitutionServiceImpl implements InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final AuthUserRepository authUserRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ShopSubscriptionDetailRepository shopSubscriptionDetailRepository;
    private final SubscriptionAccessService subscriptionAccessService;
    private final InstitutionMapper institutionMapper;
    private final AccessCatalogService accessCatalogService;

    @Override
    @Transactional
    public Institution createProfile(UUID userPublicId, CreateProfileDTO dto) {
        AuthUser user = requireUser(userPublicId);

        Institution institution = Institution.builder()
                .name(dto.name())
                .identityNumber(nextShopIdentity())
                .parentTenant(true)
                .parent(mainTenantOrNull())
                .country(dto.country())
                .traPin(dto.traPin())
                .timeZone(dto.timeZone())
                .baseCurrency(dto.baseCurrency())
                .city(dto.city())
                .createdBy(user)
                .active(true)
                .build();

        Institution saved = institutionRepository.save(institution);
        user.setInstitution(saved);
        user.getInstitutions().add(saved);
        authUserRepository.save(user);
        accessCatalogService.seedTenantRoles(saved.getPublicId(), user);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> getProfiles(String searchParam, Pageable pageable) {
        return institutionRepository.searchParentTenants(searchParam, pageable)
                .map(institutionMapper::toProfile);
    }

    @Override
    @Transactional
    public Institution updateInstitution(UUID institutionPublicId, UUID userPublicId, UpdateShopDTO dto) {
        Institution institution = requireManagedInstitution(institutionPublicId, userPublicId);
        applyUpdates(institution, dto);
        return institutionRepository.save(institution);
    }

    @Override
    @Transactional
    public Institution updateProfileNotification(UUID institutionPublicId, UUID userPublicId,
                                                 NotificationDTO notificationDTO) {
        Institution institution = requireManagedInstitution(institutionPublicId, userPublicId);
        if (notificationDTO.subscriptionPaymentList() != null
                && !notificationDTO.subscriptionPaymentList().isEmpty()) {
            NotificationPreference preference = updatePreference(
                    institution.getSubscriptionPreference(),
                    notificationDTO.subscriptionPaymentList().getFirst());
            institution.setSubscriptionPreference(notificationPreferenceRepository.save(preference));
        }
        if (notificationDTO.marketingList() != null && !notificationDTO.marketingList().isEmpty()) {
            NotificationPreference preference = updatePreference(
                    institution.getMarketingPreference(),
                    notificationDTO.marketingList().getFirst());
            institution.setMarketingPreference(notificationPreferenceRepository.save(preference));
        }
        return institutionRepository.save(institution);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse listNotificationPreferences(UUID institutionId) {
        Institution institution = requireInstitution(institutionId);
        List<NotificationPreferenceResponse.Definition> subscription = new ArrayList<>();
        if (institution.getSubscriptionPreference() != null) {
            NotificationPreference sub = institution.getSubscriptionPreference();
            subscription.add(new NotificationPreferenceResponse.Definition(
                    sub.isEmail(), sub.isSms(), sub.isPushNotification()));
        }
        List<NotificationPreferenceResponse.Definition> marketing = new ArrayList<>();
        if (institution.getMarketingPreference() != null) {
            NotificationPreference m = institution.getMarketingPreference();
            marketing.add(new NotificationPreferenceResponse.Definition(
                    m.isEmail(), m.isSms(), m.isPushNotification()));
        }
        return new NotificationPreferenceResponse(subscription, marketing);
    }

    @Override
    @Transactional
    public Institution createChildShop(UUID parentTenantId, UUID userPublicId, CreateChildShopDTO dto) {
        Institution parent = requireInstitution(parentTenantId);
        if (!parent.isParentTenant()) {
            throw new ResourceNotFoundException("Parent tenant");
        }
        if (!authUserRepository.belongsToInstitution(userPublicId, parentTenantId)) {
            throw new AccessDeniedException("You do not belong to this parent tenant");
        }
        if (!shopSubscriptionDetailRepository.hasActiveEnterpriseSubscription(
                parentTenantId, FormatDate.currentNairobiDateTime())) {
            throw new AccessDeniedException("An active Enterprise subscription is required to create child shops");
        }
        AuthUser user = requireUser(userPublicId);

        Institution child = Institution.builder()
                .name(dto.name())
                .identityNumber(nextShopIdentity())
                .parent(parent)
                .parentTenant(false)
                .country(dto.country())
                .traPin(parent.getTraPin())
                .timeZone(dto.timeZone())
                .baseCurrency(dto.baseCurrency())
                .city(dto.city())
                .createdBy(user)
                .active(true)
                .build();

        Institution saved = institutionRepository.save(child);
        user.getInstitutions().add(saved);
        authUserRepository.save(user);

        AuthUser owner = parent.getCreatedBy();
        if (owner != null && !owner.isDeleted() && !owner.getId().equals(user.getId())) {
            owner.getInstitutions().add(saved);
            authUserRepository.save(owner);
        }
        return saved;
    }

    @Override
    @Transactional
    public Institution updateChildShop(UUID parentTenantId, UUID childShopId, UUID userPublicId,
                                       UpdateShopDTO dto) {
        Institution child = requireManagedChildShop(parentTenantId, childShopId, userPublicId);
        applyUpdates(child, dto);
        return institutionRepository.save(child);
    }

    @Override
    @Transactional
    public void deleteChildShop(UUID parentTenantId, UUID childShopId, UUID userPublicId) {
        Institution child = requireManagedChildShop(parentTenantId, childShopId, userPublicId);
        authUserRepository.findByInstitutions_PublicIdAndDeletedFalse(childShopId).forEach(user -> {
            user.getInstitutions().removeIf(shop -> childShopId.equals(shop.getPublicId()));
            if (user.getInstitution() != null && childShopId.equals(user.getInstitution().getPublicId())) {
                user.setInstitution(user.getInstitutions().stream()
                        .filter(shop -> !shop.isDeleted())
                        .findFirst()
                        .orElse(null));
            }
            authUserRepository.save(user);
        });
        child.setDeleted(true);
        institutionRepository.save(child);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyShop(UUID userPublicId) {
        Institution parentShop = requireUser(userPublicId).getInstitution();
        if (parentShop == null || parentShop.isDeleted()) {
            throw new ResourceNotFoundException("Shop");
        }
        if (!parentShop.isParentTenant() && parentShop.getParent() != null) {
            parentShop = parentShop.getParent();
        }
        return institutionMapper.toProfile(parentShop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> getMyShops(UUID userPublicId) {
        return requireUser(userPublicId).getInstitutions().stream()
                .filter(shop -> !shop.isDeleted())
                .map(institutionMapper::toProfile)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopSummaryResponse> getMyShopSummaries(UUID userPublicId) {
        return requireUser(userPublicId).getInstitutions().stream()
                .filter(shop -> !shop.isDeleted())
                .map(shop -> new ShopSummaryResponse(shop.getPublicId(), shop.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChildShopResponse> getChildShops(UUID userPublicId) {
        AuthUser user = requireUser(userPublicId);
        Institution activeShop = user.getInstitution();
        if (activeShop == null) {
            throw new ResourceNotFoundException("Shop");
        }
        Institution parentTenant = activeShop.isParentTenant() ? activeShop : activeShop.getParent();
        if (parentTenant == null || parentTenant.isDeleted()) {
            throw new ResourceNotFoundException("Parent tenant");
        }
        return parentTenant.getChildren().stream()
                .filter(child -> !child.isDeleted())
                .map(institutionMapper::toChildShop)
                .toList();
    }

    @Override
    @Transactional
    public Institution deActivateShop(UUID userPublicId, UUID shopId) {
        Institution institution = requireManagedInstitution(shopId, userPublicId);
        institution.setActive(!institution.isActive());
        return institutionRepository.save(institution);
    }

    @Override
    @Transactional
    public void handleSubscription(PaymentDetail event) {
//        if (!Boolean.TRUE.equals(event.freeTrial())) {
//            throw new BadRequestException(
//                    "Paid subscriptions are handled by the finance service; use free trial here");
//        }
        subscriptionAccessService.startTrial(event.referenceId(), event.subscriptionId());
    }

    private void applyUpdates(Institution institution, UpdateShopDTO dto) {
        if (dto.name() != null && !dto.name().isBlank()) {
            institution.setName(dto.name());
        }
        if (dto.country() != null) {
            institution.setCountry(dto.country());
        }
        if (dto.traPin() != null) {
            institution.setTraPin(dto.traPin());
        }
        if (dto.timeZone() != null) {
            institution.setTimeZone(dto.timeZone());
        }
        if (dto.baseCurrency() != null) {
            institution.setBaseCurrency(dto.baseCurrency());
        }
        if (dto.city() != null) {
            institution.setCity(dto.city());
        }
    }

    private NotificationPreference updatePreference(NotificationPreference existing,
                                                    NotificationDTO.Definition definition) {
        NotificationPreference preference = existing != null ? existing : new NotificationPreference();
        preference.setEmail(definition.email());
        preference.setSms(definition.sms());
        preference.setPushNotification(definition.pushNotification());
        return preference;
    }

    private Institution requireManagedInstitution(UUID institutionPublicId, UUID userPublicId) {
        if (!authUserRepository.belongsToInstitution(userPublicId, institutionPublicId)) {
            throw new AccessDeniedException("You do not belong to this institution");
        }
        return requireInstitution(institutionPublicId);
    }

    private Institution requireManagedChildShop(UUID parentTenantId, UUID childShopId, UUID userPublicId) {
        Institution parentTenant = requireInstitution(parentTenantId);
        if (!parentTenant.isParentTenant()) {
            throw new ResourceNotFoundException("Parent tenant");
        }
        if (!authUserRepository.belongsToInstitution(userPublicId, parentTenantId)) {
            throw new AccessDeniedException("You do not belong to this parent tenant");
        }
        Institution childShop = requireInstitution(childShopId);
        if (childShop.getParent() == null || !parentTenantId.equals(childShop.getParent().getPublicId())) {
            throw new ResourceNotFoundException("Child shop");
        }
        return childShop;
    }

    private AuthUser requireUser(UUID userPublicId) {
        return authUserRepository.findByPublicIdAndDeletedIsFalse(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private Institution requireInstitution(UUID publicId) {
        return institutionRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop"));
    }

    private Institution mainTenantOrNull() {
        return institutionRepository.findByDeletedIsFalseAndMainTenantIsTrue().orElse(null);
    }

    private String nextShopIdentity() {
        return "SHOP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
