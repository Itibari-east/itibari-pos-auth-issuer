package com.itibari.auth.report;

import java.time.LocalDateTime;
import java.util.List;

public record UserAccessReport(
        String fullName,
        String email,
        String username,
        String shopName,
        LocalDateTime generatedAt,
        List<RoleAccess> roles
) {
    public record RoleAccess(String name, List<GroupAccess> groups, List<String> extraPrivileges) {
    }

    public record GroupAccess(String name, List<String> privileges) {
    }
}
