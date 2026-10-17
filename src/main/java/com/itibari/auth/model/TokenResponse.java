package com.itibari.auth.model;

public record TokenResponse(String accessToken, boolean subscription) {
}
