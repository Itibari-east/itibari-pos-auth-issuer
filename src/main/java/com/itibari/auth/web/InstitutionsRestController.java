package com.itibari.auth.web;

import com.itibari.auth.dataTypes.LoginChannel;
import com.itibari.auth.model.Institution;
import com.itibari.auth.model.TokenResponse;
import com.itibari.auth.model.request.CreateChildShopDTO;
import com.itibari.auth.model.request.CreateProfileDTO;
import com.itibari.auth.model.request.NotificationDTO;
import com.itibari.auth.model.request.PaymentDetail;
import com.itibari.auth.model.request.UpdateShopDTO;
import com.itibari.auth.model.response.*;
import com.itibari.auth.service.InstitutionService;
import com.itibari.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "institutions")
@RestController
@RequestMapping("/institutions")
@RequiredArgsConstructor
public class InstitutionsRestController {

    private final InstitutionService institutionService;
    private final TokenService tokenService;

    @PostMapping
    @Operation(summary = "Create parent tenant profile")
    public ResponseEntity<Custom2Response> createProfile(
            @Valid @RequestBody CreateProfileDTO createProfileDTO,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userPublicId = userPublicId(jwt);
        Institution institution = institutionService.createProfile(userPublicId, createProfileDTO);
        TokenResponse token = tokenService.parentShopCreatedToken(
                userPublicId, institution.getPublicId(), channel(jwt));
        return ResponseEntity.ok(new Custom2Response(
                "success",
                institution.getName(),
                institution.getPublicId(),
                token.accessToken(),
                token.subscription()));
    }

    @PostMapping("/{parentTenantId}/childShop")
    @Operation(summary = "Create child shop")
    public ResponseEntity<CustomResponse> createChildShop(
            @PathVariable UUID parentTenantId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateChildShopDTO createChildShopDTO) {
        Institution institution = institutionService.createChildShop(
                parentTenantId, userPublicId(jwt), createChildShopDTO);
        return ResponseEntity.ok(new CustomResponse("success", institution.getName()));
    }

    @PatchMapping("/{parentTenantId}/childShop/{childShopId}")
    @Operation(summary = "Update child shop")
    public ResponseEntity<CustomResponse> updateChildShop(
            @PathVariable UUID parentTenantId,
            @PathVariable UUID childShopId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateShopDTO updateShopDTO) {
        Institution institution = institutionService.updateChildShop(
                parentTenantId, childShopId, userPublicId(jwt), updateShopDTO);
        return ResponseEntity.ok(new CustomResponse("successfully updated child shop", institution.getName()));
    }

    @PatchMapping("/{institutionPublicId}")
    @Operation(summary = "Update institution")
    public ResponseEntity<CustomResponse> updateInstitution(
            @PathVariable UUID institutionPublicId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateShopDTO updateShopDTO) {
        Institution institution = institutionService.updateInstitution(
                institutionPublicId, userPublicId(jwt), updateShopDTO);
        return ResponseEntity.ok(new CustomResponse("successfully updated institution", institution.getName()));
    }

    @DeleteMapping("/{parentTenantId}/childShop/{childShopId}")
    public ResponseEntity<Void> deleteChildShop(
            @PathVariable UUID parentTenantId,
            @PathVariable UUID childShopId,
            @AuthenticationPrincipal Jwt jwt) {
        institutionService.deleteChildShop(parentTenantId, childShopId, userPublicId(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List parent tenant profiles")
    public PageResponse<ProfileResponse> getShops(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(institutionService.getProfiles(searchParam, pageable));
    }

    @GetMapping("/myShop")
    @Operation(summary = "Get my primary shop")
    public ProfileResponse getMyShop(@AuthenticationPrincipal Jwt jwt) {
        return institutionService.getMyShop(userPublicId(jwt));
    }

    @GetMapping("/myShops")
    @Operation(summary = "List my shops")
    public List<ProfileResponse> getMyShops(@AuthenticationPrincipal Jwt jwt) {
        return institutionService.getMyShops(userPublicId(jwt));
    }

    @GetMapping("/myShops/summary")
    @Operation(summary = "List my shop summaries")
    public List<ShopSummaryResponse> getMyShopSummaries(@AuthenticationPrincipal Jwt jwt) {
        return institutionService.getMyShopSummaries(userPublicId(jwt));
    }

    @PostMapping("/{institutionPublicId}/switch")
    @Operation(summary = "Switch active shop")
    public TokenResponse switchShop(
            @PathVariable UUID institutionPublicId,
            @AuthenticationPrincipal Jwt jwt) {
        return tokenService.switchShop(userPublicId(jwt), institutionPublicId, channel(jwt));
    }

    @PatchMapping({"/{institutionPublicId}/addNotification", "/{institutionPublicId}/notification-preference"})
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<CustomResponse> updateNotificationPreferences(
            @PathVariable UUID institutionPublicId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NotificationDTO notificationDTO) {
        Institution institution = institutionService.updateProfileNotification(
                institutionPublicId, userPublicId(jwt), notificationDTO);
        return ResponseEntity.ok(new CustomResponse(
                "successfully updated notification preferences for ", institution.getName()));
    }

    @GetMapping("/{institutionId}/notification-preference")
    @Operation(summary = "List notification preferences")
    public NotificationPreferenceResponse listNotificationPreferences(@PathVariable UUID institutionId) {
        return institutionService.listNotificationPreferences(institutionId);
    }

    @PostMapping("/subscriptions/{subscriptionId}/free-trial")
    @Operation(summary = "Start a free trial and return a refreshed access token")
    public TokenResponse startFreeTrial(
            @PathVariable UUID subscriptionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userPublicId = userPublicId(jwt);
        institutionService.handleSubscription(new PaymentDetail(userPublicId, subscriptionId, true));
        return tokenService.refreshToken(userPublicId, channel(jwt));
    }

    @PatchMapping("/updatePaymentAndSubscription/{subscriptionId}")
    @Operation(summary = "Start a free trial (legacy path) and return a refreshed access token")
    public TokenResponse updatePaymentAndSubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam(name = "freeTrial", required = false) boolean freeTrial,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userPublicId = userPublicId(jwt);
        institutionService.handleSubscription(new PaymentDetail(userPublicId, subscriptionId, freeTrial));
        return tokenService.refreshToken(userPublicId, channel(jwt));
    }

    @GetMapping("/childShops")
    @Operation(summary = "List child shops for active parent tenant")
    public List<ChildShopResponse> getChildShops(@AuthenticationPrincipal Jwt jwt) {
        return institutionService.getChildShops(userPublicId(jwt));
    }

    @PutMapping("/{shopId}/de-active")
    @Operation(summary = "Toggle shop active status")
    public ResponseEntity<CustomResponse> deActivateShop(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal Jwt jwt) {
        Institution institution = institutionService.deActivateShop(userPublicId(jwt), shopId);
        String message = institution.isActive()
                ? "successfully activated shop"
                : "successfully deactivated shop";
        return ResponseEntity.ok(new CustomResponse(message, institution.getName()));
    }

    private UUID userPublicId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("public_id"));
    }

    private LoginChannel channel(Jwt jwt) {
        String value = jwt.getClaimAsString("channel");
        if (value == null || value.isBlank()) {
            return LoginChannel.WEB;
        }
        try {
            return LoginChannel.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return LoginChannel.WEB;
        }
    }
}
