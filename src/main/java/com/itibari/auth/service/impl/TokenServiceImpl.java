package com.itibari.auth.service.impl;

import com.itibari.auth.dataTypes.LoginChannel;
import com.itibari.auth.exception.ForcePasswordResetRequiredException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.Institution;
import com.itibari.auth.model.TokenResponse;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.service.SubscriptionAccessService;
import com.itibari.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final long ACCESS_EXPIRY_MIN = 24 * 60;

    private final JwtEncoder jwtEncoder;
    private final AuthUserRepository authUserRepository;
    private final SubscriptionAccessService subscriptionAccessService;

    @Override
    public TokenResponse generateToken(AuthUser authUser) {
        return issueToken(authUser, LoginChannel.WEB, "CORE_CHANNEL", null);
    }

    @Override
    public TokenResponse generateGoogleToken(AuthUser authUser) {
        return issueToken(authUser, LoginChannel.WEB, "GOOGLE", null);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refreshToken(UUID userPublicId, LoginChannel channel) {
        AuthUser authUser = requireUser(userPublicId);
        return issueToken(authUser, channel, "SUBSCRIPTION_REFRESH", null);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse switchShop(UUID userPublicId, UUID institutionPublicId, LoginChannel channel) {
        AuthUser authUser = requireUser(userPublicId);
        Institution selectedShop = requireAllocatedShop(authUser, institutionPublicId);
        return issueToken(authUser, channel, "SHOP_SWITCH", selectedShop);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse parentShopCreatedToken(UUID userPublicId, UUID parentShopPublicId, LoginChannel channel) {
        AuthUser authUser = requireUser(userPublicId);
        Institution selectedShop = requireAllocatedShop(authUser, parentShopPublicId);
        return issueToken(authUser, channel, "PARENT_SHOP_CREATED", selectedShop);
    }

    private TokenResponse issueToken(AuthUser authUser, LoginChannel channel, String tokenFlow,
                                     Institution selectedShop) {
        validateUser(authUser);
        List<Institution> allocatedShops = authUser.getInstitutions() == null ? List.of()
                : authUser.getInstitutions().stream()
                .filter(shop -> !shop.isDeleted())
                .filter(Institution::isActive)
                .sorted(Comparator.comparing(Institution::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
        Institution activeShop = selectedShop != null
                ? selectedShop
                : defaultShop(authUser, allocatedShops);
        boolean hasSubscription = subscriptionAccessService.hasActiveSubscription(authUser.getPublicId());

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("pos-auth-issuer")
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_EXPIRY_MIN, ChronoUnit.MINUTES))
                .subject(authUser.getUsername())
                .claim("public_id", authUser.getPublicId().toString())
                .claim("user_id", authUser.getId() == null ? "" : authUser.getId())
                .claim("full_name", authUser.fullName())
                .claim("phone_number", authUser.getPhoneNumber() != null ? authUser.getPhoneNumber() : "")
                .claim("admin", authUser.isAdmin())
                .claim("force_password_reset", authUser.isForcePasswordReset())
                .claim("subscription", hasSubscription)
                .claim("channel", channel.name())
                .claim("token_flow", tokenFlow)
                .claim("active_shop", activeShop != null ? activeShop.getPublicId().toString() : "")
                .claim("shops", allocatedShops.stream().map(shop -> shop.getPublicId().toString()).toList())
                .claim("roles", authUser.roleNames())
                .claim("warehouses", List.of())
                .claim("authorities", authUser.authorityNames())
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new TokenResponse(accessToken, hasSubscription);
    }

    private Institution defaultShop(AuthUser authUser, List<Institution> allocatedShops) {
        if (authUser.getInstitution() != null
                && !authUser.getInstitution().isDeleted()
                && authUser.getInstitution().isActive()) {
            return authUser.getInstitution();
        }
        return allocatedShops.isEmpty() ? null : allocatedShops.getFirst();
    }

    private Institution requireAllocatedShop(AuthUser authUser, UUID institutionPublicId) {
        return authUser.getInstitutions().stream()
                .filter(shop -> !shop.isDeleted())
                .filter(Institution::isActive)
                .filter(shop -> institutionPublicId.equals(shop.getPublicId()))
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("Shop is not allocated to user"));
    }

    private AuthUser requireUser(UUID userPublicId) {
        return authUserRepository.findByPublicIdAndDeletedIsFalse(userPublicId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }

    private void validateUser(AuthUser user) {
        if (!user.isAccountNonLocked()) {
            throw new LockedException("User is locked");
        }
        if (!user.isEnabled() || !user.isActiveStatus()) {
            throw new DisabledException("User is disabled");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new DisabledException("User credentials have expired");
        }
        if (user.isForcePasswordReset()) {
            throw new ForcePasswordResetRequiredException();
        }
    }
}
