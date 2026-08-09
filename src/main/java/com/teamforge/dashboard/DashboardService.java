package com.teamforge.dashboard;

import com.teamforge.projects.Project;
import com.teamforge.projects.ProjectRepository;
import com.teamforge.projects.ProjectSkill;
import com.teamforge.projects.ProjectSkillRepository;
import com.teamforge.projects.ProjectStatus;
import com.teamforge.skills.Skill;
import com.teamforge.skills.SkillRepository;
import com.teamforge.skills.UserSkill;
import com.teamforge.skills.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    // A trending/gap skill is still considered a real shortage while 2 or
    // fewer people in the whole company hold it.
    private static final long GAP_THRESHOLD = 2;

    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProjectSkillRepository projectSkillRepository;

    public DashboardStatsDto getStats() {
        List<Project> allProjects = projectRepository.findAll();
        List<Project> activeProjectsList = allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                .toList();

        long activeProjectsCount = activeProjectsList.size();
        long skillsetCount = skillRepository.count();

        List<UserSkill> allUserSkills = userSkillRepository.findAll();
        List<Skill> allSkills = skillRepository.findAll();
        List<ProjectSkill> allProjectSkills = projectSkillRepository.findAll();

        // Employees per skill.
        Map<String, Long> skillsetDistribution = allUserSkills.stream()
                .collect(Collectors.groupingBy(us -> us.getSkill().getName(), Collectors.counting()));

        // How many DISTINCT projects (any status) currently list this skill as required.
        Map<String, Set<Long>> projectsPerSkill = new HashMap<>();
        // Same, but only counting projects that are still PLANNING or ACTIVE (current demand).
        Map<String, Set<Long>> openProjectsPerSkill = new HashMap<>();
        for (ProjectSkill ps : allProjectSkills) {
            String skillName = ps.getSkill().getName();
            projectsPerSkill.computeIfAbsent(skillName, k -> new HashSet<>()).add(ps.getProject().getId());
            if (ps.getProject().getStatus() != ProjectStatus.COMPLETED) {
                openProjectsPerSkill.computeIfAbsent(skillName, k -> new HashSet<>()).add(ps.getProject().getId());
            }
        }

        Map<String, Long> skillProjectUsage = new HashMap<>();
        projectsPerSkill.forEach((name, ids) -> skillProjectUsage.put(name, (long) ids.size()));

        // Skill Gap: skills flagged as trending in the market that we don't have enough of internally.
        Map<String, Long> skillGapDistribution = new HashMap<>();
        Map<String, Long> skillGapDemand = new HashMap<>();
        long skillGapCount = 0;

        for (Skill skill : allSkills) {
            if (skill.isTrending()) {
                long employeeCount = skillsetDistribution.getOrDefault(skill.getName(), 0L);
                long demandCount = openProjectsPerSkill.getOrDefault(skill.getName(), Set.of()).size();
                skillGapDistribution.put(skill.getName(), employeeCount);
                skillGapDemand.put(skill.getName(), demandCount);
                if (employeeCount <= GAP_THRESHOLD) {
                    skillGapCount++;
                }
            }
        }

        // Progress of every project relative to its deadline (used by the
        // Active Projects detail view). 100% once completed or past end date.
        List<DashboardStatsDto.ProjectProgressDto> projectProgress = allProjects.stream()
                .sorted(Comparator.comparing(Project::getTitle))
                .map(this::toProgressDto)
                .toList();

        return DashboardStatsDto.builder()
                .activeProjects(activeProjectsCount)
                .skillsetCount(skillsetCount)
                .skillGapCount(skillGapCount)
                .skillsetDistribution(skillsetDistribution)
                .skillProjectUsage(skillProjectUsage)
                .skillGapDistribution(skillGapDistribution)
                .skillGapDemand(skillGapDemand)
                .projectProgress(projectProgress)
                .build();
    }

    private DashboardStatsDto.ProjectProgressDto toProgressDto(Project p) {
        Integer percent = null;
        Long daysRemaining = null;

        if (p.getStatus() == ProjectStatus.COMPLETED) {
            percent = 100;
            if (p.getEndDate() != null) {
                daysRemaining = 0L;
            }
        } else if (p.getStartDate() != null && p.getEndDate() != null) {
            long totalDays = ChronoUnit.DAYS.between(p.getStartDate(), p.getEndDate());
            long elapsedDays = ChronoUnit.DAYS.between(p.getStartDate(), LocalDate.now());
            if (totalDays <= 0) {
                percent = elapsedDays >= 0 ? 100 : 0;
            } else {
                long raw = Math.round((elapsedDays * 100.0) / totalDays);
                percent = (int) Math.max(0, Math.min(100, raw));
            }
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), p.getEndDate());
        }

        return new DashboardStatsDto.ProjectProgressDto(
                p.getId(), p.getTitle(), p.getStatus().name(), p.getStartDate(), p.getEndDate(), percent, daysRemaining
        );
    }
}
