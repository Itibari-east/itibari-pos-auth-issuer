package com.itibari.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param publicBaseUrl Gateway base used to rewrite local {@code file:} URLs into browser-loadable
 *                      content URLs (e.g. {@code https://api-pos-dev.itibari.io/api/v1/pos}).
 */
@ConfigurationProperties(prefix = "pos.files")
public record FileClientProperties(@DefaultValue("") String publicBaseUrl) {
}
