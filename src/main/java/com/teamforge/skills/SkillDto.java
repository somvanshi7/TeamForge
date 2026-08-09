package com.teamforge.skills;

import lombok.Builder;

@Builder
public record SkillDto(Long id, String name, String category, boolean isTrending) {
    public static SkillDto fromEntity(Skill skill) {
        return builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory())
                .isTrending(skill.isTrending())
                .build();
    }
}
