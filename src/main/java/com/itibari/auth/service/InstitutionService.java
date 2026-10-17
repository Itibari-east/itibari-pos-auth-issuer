package com.itibari.auth.service;

import com.itibari.auth.model.Institution;
import com.itibari.auth.model.request.CreateChildShopDTO;
import com.itibari.auth.model.request.CreateProfileDTO;
import com.itibari.auth.model.request.NotificationDTO;
import com.itibari.auth.model.request.PaymentDetail;
import com.itibari.auth.model.request.UpdateShopDTO;
import com.itibari.auth.model.response.ChildShopResponse;
import com.itibari.auth.model.response.NotificationPreferenceResponse;
import com.itibari.auth.model.response.ProfileResponse;
import com.itibari.auth.model.response.ShopSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InstitutionService {

    Institution createProfile(UUID userPublicId, @Valid CreateProfileDTO createProfileDTO);

    Page<ProfileResponse> getProfiles(String searchParam, Pageable pageable);

    Institution updateInstitution(UUID institutionPublicId, UUID userPublicId, @Valid UpdateShopDTO updateShopDTO);

    Institution updateProfileNotification(UUID institutionPublicId, UUID userPublicId,
                                          @Valid NotificationDTO notificationDTO);

    NotificationPreferenceResponse listNotificationPreferences(UUID institutionId);

    Institution createChildShop(UUID parentTenantId, UUID userPublicId,
                                @Valid CreateChildShopDTO createChildShopDTO);

    Institution updateChildShop(UUID parentTenantId, UUID childShopId, UUID userPublicId,
                                @Valid UpdateShopDTO updateShopDTO);

    void deleteChildShop(UUID parentTenantId, UUID childShopId, UUID userPublicId);

    ProfileResponse getMyShop(UUID userPublicId);

    List<ProfileResponse> getMyShops(UUID userPublicId);

    List<ShopSummaryResponse> getMyShopSummaries(UUID userPublicId);

    List<ChildShopResponse> getChildShops(UUID userPublicId);

    Institution deActivateShop(UUID userPublicId, UUID shopId);

    void handleSubscription(PaymentDetail event);
}
