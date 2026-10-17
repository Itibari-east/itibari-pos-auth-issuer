package com.itibari.auth.web;

import com.itibari.auth.model.Subscription;
import com.itibari.auth.model.request.CreateSubscriptionDTO;
import com.itibari.auth.model.response.CustomResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.model.response.SubscriptionResponse;
import com.itibari.auth.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "subscriptions")
@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionRestController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Create or update subscription plan")
    public ResponseEntity<CustomResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionDTO createSubscriptionDTO) {
        Subscription subscription = subscriptionService.createSubscription(createSubscriptionDTO);
        return ResponseEntity.ok(new CustomResponse("success", subscription.getName().getLabel()));
    }

    @GetMapping
    @Operation(summary = "List subscription plans")
    public PageResponse<SubscriptionResponse> getSubscriptions(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(subscriptionService.getSubscriptions(searchParam, pageable));
    }
}
