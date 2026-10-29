package com.itibari.auth.service.impl;
import com.itibari.auth.repository.ShopSubscriptionDetailRepository;
import com.itibari.auth.service.SubscriptionAccessService;
import com.itibari.auth.util.date.FormatDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class SubscriptionAccessServiceImpl implements SubscriptionAccessService {
    private final ShopSubscriptionDetailRepository detailRepository;
    @Override
    @Transactional(readOnly=true)
    public boolean hasActiveSubscription(UUID userPublicId) {
        return detailRepository.hasActiveSubscriptionForUser(userPublicId, FormatDate.currentNairobiDateTime());
    }
}
