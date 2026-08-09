package com.teamforge.projects;

import com.teamforge.common.BaseEntity;
import com.teamforge.skills.ProficiencyLevel;
import com.teamforge.skills.Skill;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProjectSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProficiencyLevel requiredProficiency;
}
