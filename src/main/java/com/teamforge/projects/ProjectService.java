package com.teamforge.projects;

import com.teamforge.users.User;
import com.teamforge.users.UserRepository;
import com.teamforge.notifications.Notification;
import com.teamforge.notifications.NotificationRepository;
import com.teamforge.skills.Skill;
import com.teamforge.skills.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final SkillRepository skillRepository;

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request) {
        if (projectRepository.existsByTitleIgnoreCase(request.title())) {
            throw new IllegalStateException("Project with this title already exists");
        }

        Project project = Project.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        project = projectRepository.save(project);
        saveRequiredSkills(project, request.requiredSkills());

        return ProjectDto.fromEntity(project, projectSkillRepository.findByProject(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(p -> ProjectDto.fromEntity(p, projectSkillRepository.findByProject(p)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDto getProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found"));
        return ProjectDto.fromEntity(project, projectSkillRepository.findByProject(project));
    }

    @Transactional
    public ProjectDto updateProject(Long projectId, CreateProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found"));

        if (!project.getTitle().equalsIgnoreCase(request.title()) &&
            projectRepository.existsByTitleIgnoreCase(request.title())) {
            throw new IllegalStateException("Project with this title already exists");
        }

        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setStatus(request.status());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project = projectRepository.save(project);

        // Simplest way to sync required skills is to replace the whole set.
        List<ProjectSkill> oldSkills = projectSkillRepository.findByProject(project);
        projectSkillRepository.deleteAll(oldSkills);
        saveRequiredSkills(project, request.requiredSkills());

        return ProjectDto.fromEntity(project, projectSkillRepository.findByProject(project));
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found"));

        List<ProjectMember> members = projectMemberRepository.findByProject(project);
        projectMemberRepository.deleteAll(members);

        List<ProjectSkill> skills = projectSkillRepository.findByProject(project);
        projectSkillRepository.deleteAll(skills);

        projectRepository.delete(project);
    }

    @Transactional
    public ProjectMemberDto addMemberToProject(AddProjectMemberRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new IllegalStateException("Project not found"));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (projectMemberRepository.existsByUserAndProject(user, project)) {
            throw new IllegalStateException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(request.role())
                .build();

        member = projectMemberRepository.save(member);

        Notification notification = Notification.builder()
                .user(user)
                .message("You have been shortlisted for the project: " + project.getTitle() + " as a " + request.role())
                .isRead(false)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        notificationRepository.save(notification);

        return ProjectMemberDto.fromEntity(member);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberDto> getProjectMembers(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found"));

        return projectMemberRepository.findByProject(project).stream()
                .map(ProjectMemberDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getMyProjects(User user) {
        return projectMemberRepository.findByUser(user).stream()
                .map(pm -> pm.getProject())
                .map(p -> ProjectDto.fromEntity(p, projectSkillRepository.findByProject(p)))
                .collect(Collectors.toList());
    }

    private void saveRequiredSkills(Project project, List<CreateProjectSkillRequest> requiredSkills) {
        if (requiredSkills == null) return;
        for (CreateProjectSkillRequest psr : requiredSkills) {
            Skill skill = skillRepository.findById(psr.skillId())
                    .orElseThrow(() -> new IllegalStateException("Skill not found"));
            ProjectSkill ps = ProjectSkill.builder()
                    .project(project)
                    .skill(skill)
                    .requiredProficiency(psr.requiredProficiency())
                    .build();
            projectSkillRepository.save(ps);
        }
    }
}
