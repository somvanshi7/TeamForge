package com.teamforge.skills;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record TalentSearchResultDto(
    Long userId,
    String fullName,
    String email,
    String department,
    String designation,
    ProficiencyLevel proficiencyLevel,
    Integer yearsOfExperience,
    boolean availableNow,
    LocalDate availableFrom
) {}
