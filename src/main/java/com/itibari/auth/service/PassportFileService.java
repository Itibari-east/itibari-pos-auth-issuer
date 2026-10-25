package com.itibari.auth.service;

import com.itibari.auth.client.file.FileFeignClient;
import com.itibari.auth.config.FileClientProperties;
import com.itibari.auth.model.response.AuthUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassportFileService {

    private static final String PASSPORT_REFERENCE_TYPE = "PASSPORT";

    private final FileFeignClient fileFeignClient;
    private final FileClientProperties fileClientProperties;

    public AuthUserResponse attach(AuthUserResponse user) {
        if (user == null || user.publicId() == null) {
            return user;
        }
        String url = urls(List.of(user.publicId())).get(user.publicId());
        return user.withPassportUrl(toBrowserUrl(url));
    }

    public Page<AuthUserResponse> attach(Page<AuthUserResponse> page) {
        if (page == null || page.isEmpty()) {
            return page;
        }
        Map<UUID, String> urls = urls(page.getContent().stream()
                .map(AuthUserResponse::publicId)
                .filter(id -> id != null)
                .distinct()
                .toList());
        return page.map(user -> user.withPassportUrl(toBrowserUrl(urls.get(user.publicId()))));
    }

    private Map<UUID, String> urls(List<UUID> userPublicIds) {
        if (userPublicIds == null || userPublicIds.isEmpty()) {
            return Map.of();
        }
        try {
            Map<UUID, String> fetched = fileFeignClient.latestFileUrls(PASSPORT_REFERENCE_TYPE, userPublicIds);
            return fetched == null ? Map.of() : fetched;
        } catch (Exception ex) {
            log.warn("Unable to resolve passport file URLs: {}", ex.toString());
            return Map.of();
        }
    }

    private String toBrowserUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (!url.startsWith("file:")) {
            return url;
        }
        String base = fileClientProperties.publicBaseUrl();
        if (base == null || base.isBlank()) {
            return url;
        }
        int slash = url.lastIndexOf('/');
        if (slash < 0 || slash == url.length() - 1) {
            return url;
        }
        String filePublicId = url.substring(slash + 1);
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized + "/files/" + filePublicId + "/content";
    }
}
