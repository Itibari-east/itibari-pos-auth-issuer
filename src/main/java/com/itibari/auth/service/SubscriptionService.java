package com.itibari.auth.service;

import com.itibari.auth.model.Subscription;
import com.itibari.auth.model.request.CreateSubscriptionDTO;
import com.itibari.auth.model.response.SubscriptionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscriptionService {
    Subscription createSubscription(@Valid CreateSubscriptionDTO createSubscriptionDTO);

    Page<SubscriptionResponse> getSubscriptions(String searchParam, Pageable pageable);
}
