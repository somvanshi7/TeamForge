package com.teamforge.projects;

import com.teamforge.users.UserProfileResponse;
import lombok.Builder;

@Builder
public record ProjectMemberDto(Long id, ProjectDto project, UserProfileResponse user, ProjectRole role) {
    public static ProjectMemberDto fromEntity(ProjectMember member) {
        return builder()
                .id(member.getId())
                .project(ProjectDto.fromEntity(member.getProject()))
                .user(new UserProfileResponse(
                        member.getUser().getId(),
                        member.getUser().getFullName(),
                        member.getUser().getEmail(),
                        member.getUser().getDepartment(),
                        member.getUser().getDesignation(),
                        member.getUser().getRole().name(),
                        null
                ))
                .role(member.getRole())
                .build();
    }
}
