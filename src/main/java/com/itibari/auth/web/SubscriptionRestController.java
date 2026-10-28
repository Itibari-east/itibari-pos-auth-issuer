package com.itibari.auth.web;

import com.itibari.auth.client.licensing.LicensingFeignClient;
import com.itibari.auth.model.request.CreateSubscriptionDTO;
import com.itibari.auth.model.response.CustomResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.model.response.SubscriptionResponse;
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


    /**
     * moved subscriptions to licensing service
     */
    private final LicensingFeignClient licensing;

    @PostMapping
    @Operation(summary = "Create or update subscription plan")
    public ResponseEntity<CustomResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionDTO createSubscriptionDTO) {
        return ResponseEntity.ok(licensing.create(createSubscriptionDTO));
    }

    @GetMapping
    @Operation(summary = "List subscription plans")
    public PageResponse<SubscriptionResponse> getSubscriptions(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20) Pageable pageable) {
        return licensing.plans(searchParam, pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().stream().map(order -> order.getProperty() + "," + order.getDirection()).toList());
    }
}
