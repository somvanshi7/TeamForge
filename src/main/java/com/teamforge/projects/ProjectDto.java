package com.teamforge.projects;

import lombok.Builder;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record ProjectDto(
    Long id,
    String title,
    String description,
    ProjectStatus status,
    LocalDate startDate,
    LocalDate endDate,
    List<ProjectSkillDto> requiredSkills
) {
    public static ProjectDto fromEntity(Project project) {
        return fromEntity(project, Collections.emptyList());
    }

    public static ProjectDto fromEntity(Project project, List<ProjectSkill> skills) {
        List<ProjectSkillDto> skillDtos = skills == null
                ? Collections.emptyList()
                : skills.stream().map(ProjectSkillDto::fromEntity).collect(Collectors.toList());

        return builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .requiredSkills(skillDtos)
                .build();
    }
}
