package com.teamforge.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateProjectRequest(
    @NotBlank(message = "Title is required") String title,
    @NotBlank(message = "Description is required") String description,
    @NotNull(message = "Status is required") ProjectStatus status,
    LocalDate startDate,
    LocalDate endDate,
    List<CreateProjectSkillRequest> requiredSkills
) {}
