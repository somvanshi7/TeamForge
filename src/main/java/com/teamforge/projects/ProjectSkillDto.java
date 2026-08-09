package com.teamforge.projects;

import com.teamforge.skills.ProficiencyLevel;
import lombok.Builder;

@Builder
public record ProjectSkillDto(Long skillId, String skillName, String category, ProficiencyLevel requiredProficiency) {
    public static ProjectSkillDto fromEntity(ProjectSkill ps) {
        return builder()
                .skillId(ps.getSkill().getId())
                .skillName(ps.getSkill().getName())
                .category(ps.getSkill().getCategory())
                .requiredProficiency(ps.getRequiredProficiency())
                .build();
    }
}
