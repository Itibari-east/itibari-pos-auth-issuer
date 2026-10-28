package com.itibari.auth.client.licensing;

import com.itibari.auth.model.request.CreateSubscriptionDTO;
import com.itibari.auth.model.request.SubscriptionPaymentRequest;
import com.itibari.auth.model.response.CustomResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.model.response.SubscriptionPaymentResponse;
import com.itibari.auth.model.response.SubscriptionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "licensing-service", url = "${licensing.service.url:}")
public interface LicensingFeignClient {
    @PostMapping("/subscription")
    CustomResponse create(@RequestBody CreateSubscriptionDTO request);

    @GetMapping("/subscription")
    PageResponse<SubscriptionResponse> plans(@RequestParam(value = "searchParam", required = false) String search,
                                             @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") List<String> sort);

    @PostMapping("/subscription/{subscriptionId}/free-trial")
    void startTrial(@PathVariable("subscriptionId") UUID subscriptionId);

    @PostMapping("/subscription/payments/mpesa")
    SubscriptionPaymentResponse initiate(@RequestBody SubscriptionPaymentRequest request);

    @PostMapping("/subscription/payments/{paymentId}/reconcile")
    SubscriptionPaymentResponse reconcile(@PathVariable("paymentId") UUID paymentId);
}
