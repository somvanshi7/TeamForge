package com.teamforge.projects;

import com.teamforge.skills.ProficiencyLevel;
import jakarta.validation.constraints.NotNull;

public record CreateProjectSkillRequest(
    @NotNull Long skillId,
    @NotNull ProficiencyLevel requiredProficiency
) {}
