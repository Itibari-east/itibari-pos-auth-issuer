package com.itibari.auth.client.file;

import com.itibari.auth.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "pos-file", configuration = FeignConfig.class)
public interface FileFeignClient {

    @GetMapping("/files/latest-urls")
    Map<UUID, String> latestFileUrls(@RequestParam("referenceType") String referenceType,
                                     @RequestParam("referenceIds") List<UUID> referenceIds);
}
