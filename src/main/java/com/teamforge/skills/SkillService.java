package com.teamforge.skills;

import com.teamforge.users.User;
import com.teamforge.users.UserRepository;
import com.teamforge.users.UserProfileResponse;
import com.teamforge.projects.ProjectMemberRepository;
import com.teamforge.projects.ProjectRepository;
import com.teamforge.projects.ProjectSkillRepository;
import com.teamforge.projects.ProjectSkill;
import com.teamforge.projects.Project;
import com.teamforge.projects.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class SkillService {

    // Employees currently engaged on an ACTIVE project are only surfaced as
    // candidates for a new project if their current commitment frees up within
    // this many days from "now" (the moment the search is run).
    private static final long AVAILABILITY_WINDOW_DAYS = 7;

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectSkillRepository projectSkillRepository;

    @Transactional(readOnly = true)
    public List<SkillDto> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(SkillDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SkillDto createSkill(CreateSkillRequest request) {
        if (skillRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalStateException("Skill already exists");
        }
        // A skill only gets added to the catalog manually (outside the seed data)
        // because a project needs it and nobody has it yet - so it is, by
        // definition, a market/skill-gap entry until employees pick it up.
        Skill skill = Skill.builder()
                .name(request.name())
                .category(request.category())
                .isTrending(true)
                .build();
        skill = skillRepository.save(skill);
        return SkillDto.fromEntity(skill);
    }

    @Transactional(readOnly = true)
    public List<UserSkillDto> getUserSkills(User user) {
        return userSkillRepository.findByUser(user).stream()
                .map(UserSkillDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserSkillDto addUserSkill(User user, AddUserSkillRequest request) {
        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new IllegalStateException("Skill not found"));

        if (userSkillRepository.existsByUserAndSkill(user, skill)) {
            throw new IllegalStateException("User already has this skill");
        }

        UserSkill userSkill = UserSkill.builder()
                .user(user)
                .skill(skill)
                .proficiencyLevel(request.proficiencyLevel())
                .yearsOfExperience(request.yearsOfExperience())
                .build();

        userSkill = userSkillRepository.save(userSkill);
        return UserSkillDto.fromEntity(userSkill);
    }

    @Transactional
    public void removeUserSkill(User user, Long userSkillId) {
        UserSkill userSkill = userSkillRepository.findById(userSkillId)
                .orElseThrow(() -> new IllegalStateException("UserSkill not found"));

        if (!userSkill.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Not authorized to remove this skill");
        }

        userSkillRepository.delete(userSkill);
    }

    /**
     * Talent Search: given a single skill, list every employee who has it,
     * along with their proficiency, department and email, plus whether they
     * are free right now or still tied up on an active project.
     */
    @Transactional(readOnly = true)
    public List<TalentSearchResultDto> searchUsersBySkill(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalStateException("Skill not found"));

        return userSkillRepository.findBySkill(skill).stream()
                .map(us -> {
                    User u = us.getUser();
                    LocalDate availableFrom = latestActiveProjectEndDate(u);
                    boolean availableNow = availableFrom == null || !availableFrom.isAfter(LocalDate.now());
                    return TalentSearchResultDto.builder()
                            .userId(u.getId())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .department(u.getDepartment())
                            .designation(u.getDesignation())
                            .proficiencyLevel(us.getProficiencyLevel())
                            .yearsOfExperience(us.getYearsOfExperience())
                            .availableNow(availableNow)
                            .availableFrom(availableNow ? null : availableFrom)
                            .build();
                })
                .sorted(Comparator.comparing(TalentSearchResultDto::proficiencyLevel).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Team assembly for a project: only surface employees who satisfy every
     * required skill at the required proficiency, AND are either free right
     * now or wrapping up their current active project within the next
     * AVAILABILITY_WINDOW_DAYS days from the moment this search is run.
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsersForProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found"));

        List<ProjectSkill> requiredSkills = projectSkillRepository.findByProject(project);
        LocalDate now = LocalDate.now();

        return userRepository.findAll().stream()
                .filter(u -> hasRequiredSkills(u, requiredSkills))
                .map(u -> new Object[]{u, latestActiveProjectEndDate(u)})
                .filter(arr -> {
                    LocalDate availableFrom = (LocalDate) arr[1];
                    // Include if available right now, OR they free up within the window.
                    return availableFrom == null || !availableFrom.isAfter(now.plusDays(AVAILABILITY_WINDOW_DAYS));
                })
                .map(arr -> {
                    User u = (User) arr[0];
                    LocalDate availableFrom = (LocalDate) arr[1];
                    return new UserProfileResponse(
                        u.getId(), u.getFullName(), u.getEmail(),
                        u.getDepartment(), u.getDesignation(), u.getRole().name(), availableFrom
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * The date the user's current active-project commitment ends, or null if
     * they are not tied up on any active project right now.
     */
    private LocalDate latestActiveProjectEndDate(User user) {
        LocalDate availableFrom = projectMemberRepository.findByUser(user).stream()
                .filter(pm -> pm.getProject().getStatus() == ProjectStatus.ACTIVE && pm.getProject().getEndDate() != null)
                .map(pm -> pm.getProject().getEndDate())
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (availableFrom != null && availableFrom.isBefore(LocalDate.now())) {
            return null; // their active project already ran past its end date - treat as available
        }
        return availableFrom;
    }

    private boolean hasRequiredSkills(User user, List<ProjectSkill> requiredSkills) {
        if (requiredSkills.isEmpty()) return true; // no requirements

        List<UserSkill> userSkills = userSkillRepository.findByUser(user);

        for (ProjectSkill req : requiredSkills) {
            boolean hasSkill = userSkills.stream().anyMatch(us ->
                us.getSkill().getId().equals(req.getSkill().getId()) &&
                us.getProficiencyLevel().ordinal() >= req.getRequiredProficiency().ordinal()
            );
            if (!hasSkill) return false;
        }
        return true;
    }
}
