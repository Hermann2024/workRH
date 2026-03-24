package com.workrh.users.api.dto;

import java.util.Set;

public record LoginResponse(String accessToken, String tenantId, Set<String> roles) {
}
