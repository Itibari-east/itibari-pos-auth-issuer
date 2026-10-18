package com.itibari.auth.client.notification;

import com.itibari.auth.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", configuration = FeignConfig.class)
public interface NotificationFeignClient {

    @PostMapping("/notifications/send")
    NotificationResponse send(@RequestBody SendNotificationRequest request);
}
