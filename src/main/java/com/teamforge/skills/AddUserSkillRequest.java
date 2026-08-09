package com.teamforge.skills;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record AddUserSkillRequest(
    @NotNull(message = "Skill ID is required") Long skillId,
    @NotNull(message = "Proficiency level is required") ProficiencyLevel proficiencyLevel,
    @NotNull(message = "Years of experience is required") @Min(0) Integer yearsOfExperience
) {}
