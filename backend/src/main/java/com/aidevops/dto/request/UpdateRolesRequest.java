package com.aidevops.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateRolesRequest(
    @NotEmpty(message = "At least one role must be provided")
    Set<String> roles
) {}
