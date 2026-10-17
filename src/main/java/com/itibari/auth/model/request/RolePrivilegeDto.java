package com.itibari.auth.model.request;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record RolePrivilegeDto(List<UUID> privilegeUUIDs) {
}
