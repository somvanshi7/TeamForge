package com.teamforge.projects;

import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(
    @NotNull(message = "Project ID is required") Long projectId,
    @NotNull(message = "User ID is required") Long userId,
    @NotNull(message = "Role is required") ProjectRole role
) {}
