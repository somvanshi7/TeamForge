package com.teamforge.skills;

import lombok.Builder;

@Builder
public record UserSkillDto(Long id, SkillDto skill, ProficiencyLevel proficiencyLevel, Integer yearsOfExperience) {
    public static UserSkillDto fromEntity(UserSkill userSkill) {
        return builder()
                .id(userSkill.getId())
                .skill(SkillDto.fromEntity(userSkill.getSkill()))
                .proficiencyLevel(userSkill.getProficiencyLevel())
                .yearsOfExperience(userSkill.getYearsOfExperience())
                .build();
    }
}
