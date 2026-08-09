package com.teamforge;

import com.teamforge.projects.Project;
import com.teamforge.projects.ProjectRepository;
import com.teamforge.projects.ProjectStatus;
import com.teamforge.projects.ProjectSkill;
import com.teamforge.projects.ProjectSkillRepository;
import com.teamforge.skills.Skill;
import com.teamforge.skills.SkillRepository;
import com.teamforge.skills.UserSkill;
import com.teamforge.skills.UserSkillRepository;
import com.teamforge.skills.ProficiencyLevel;
import com.teamforge.users.Role;
import com.teamforge.users.User;
import com.teamforge.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProjectRepository projectRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 1) {
            return; // Already seeded (ignoring the 1 user the user might have created)
        }

        System.out.println("SEEDING DATABASE WITH ENTERPRISE DATA...");

        String password = passwordEncoder.encode("password123");

        // Create HR
        User hr = createUser("Sarah Jenkins", "hr@teamforge.com", password, "Human Resources", "HR Director", Role.ADMIN);

        // Create Managers
        User m1 = createUser("David Miller", "manager1@teamforge.com", password, "Engineering", "Engineering Manager", Role.MANAGER);
        User m2 = createUser("Jessica Wong", "manager2@teamforge.com", password, "Design", "Design Lead", Role.MANAGER);
        
        List<User> employees = Arrays.asList(
            createUser("Alex Chen", "alex@teamforge.com", password, "Engineering", "Backend Developer", Role.EMPLOYEE),
            createUser("Sam Smith", "sam@teamforge.com", password, "Engineering", "Frontend Developer", Role.EMPLOYEE),
            createUser("Taylor Swift", "taylor@teamforge.com", password, "Marketing", "Marketing Specialist", Role.EMPLOYEE),
            createUser("Jordan Lee", "jordan@teamforge.com", password, "Engineering", "DevOps Engineer", Role.EMPLOYEE),
            createUser("Casey Jones", "casey@teamforge.com", password, "Design", "UX Designer", Role.EMPLOYEE),
            createUser("Morgan Freeman", "morgan@teamforge.com", password, "Sales", "Sales Executive", Role.EMPLOYEE),
            createUser("Jamie Foxx", "jamie@teamforge.com", password, "Engineering", "Fullstack Developer", Role.EMPLOYEE),
            createUser("Riley Reid", "riley@teamforge.com", password, "Design", "UI Designer", Role.EMPLOYEE),
            createUser("Avery Johnson", "avery@teamforge.com", password, "Engineering", "QA Engineer", Role.EMPLOYEE),
            createUser("Quinn Fabray", "quinn@teamforge.com", password, "Marketing", "Content Writer", Role.EMPLOYEE)
        );

        // Create Global Skills
        List<Skill> skills = Arrays.asList(
            createSkill("Java", "Technical", true),
            createSkill("Python", "Technical", true),
            createSkill("React", "Technical", false),
            createSkill("Docker", "Technical", false),
            createSkill("Figma", "Design", false),
            createSkill("Leadership", "Interpersonal", true),
            createSkill("Public Speaking", "Interpersonal", false),
            createSkill("SEO", "Marketing", false),
            createSkill("Rust", "Technical", true), // High demand, skill gap expected
            createSkill("Kubernetes", "Technical", true)
        );

        // Assign random skills to employees
        for (User employee : employees) {
            int numSkills = random.nextInt(3) + 1; // 1 to 3 skills
            for (int i = 0; i < numSkills; i++) {
                Skill skill = skills.get(random.nextInt(skills.size()));
                if (!userSkillRepository.existsByUserAndSkill(employee, skill)) {
                    UserSkill userSkill = UserSkill.builder()
                            .user(employee)
                            .skill(skill)
                            .proficiencyLevel(ProficiencyLevel.values()[random.nextInt(ProficiencyLevel.values().length)])
                            .yearsOfExperience(random.nextInt(10) + 1)
                            .build();
                    userSkillRepository.save(userSkill);
                }
            }
        }

        // Create Projects
        Project p1 = createProject("Mars Rover Rewrite", "Rewriting the Rover in Rust", ProjectStatus.ACTIVE, LocalDate.now().minusDays(10), LocalDate.now().plusDays(40));
        Project p2 = createProject("Website Redesign", "Overhaul the corporate site", ProjectStatus.PLANNING, LocalDate.now().plusDays(5), LocalDate.now().plusDays(60));
        Project p3 = createProject("Q3 Marketing Push", "SEO and Ads", ProjectStatus.COMPLETED, LocalDate.now().minusDays(90), LocalDate.now().minusDays(10));
        Project p4 = createProject("AI Talent Matcher", "Using AI to match talent", ProjectStatus.ACTIVE, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5)); // Due soon (within 10 days!)
        
        // Add Project Skills
        addProjectSkill(p1, skills.get(8), ProficiencyLevel.ADVANCED); // Rust
        addProjectSkill(p2, skills.get(2), ProficiencyLevel.INTERMEDIATE); // React
        addProjectSkill(p4, skills.get(1), ProficiencyLevel.ADVANCED); // Python
        
        System.out.println("SEEDING COMPLETE.");
    }

    private User createUser(String name, String email, String password, String dept, String title, Role role) {
        if(userRepository.findByEmail(email).isPresent()) return userRepository.findByEmail(email).get();
        User u = User.builder().fullName(name).email(email).password(password).department(dept).designation(title).role(role).build();
        return userRepository.save(u);
    }

    private Skill createSkill(String name, String category, boolean isTrending) {
        Skill skill = Skill.builder().name(name).category(category).isTrending(isTrending).build();
        return skillRepository.save(skill);
    }

    private Project createProject(String title, String desc, ProjectStatus status, LocalDate start, LocalDate end) {
        Project project = Project.builder().title(title).description(desc).status(status).startDate(start).endDate(end).build();
        return projectRepository.save(project);
    }
    
    private void addProjectSkill(Project project, Skill skill, ProficiencyLevel proficiency) {
        ProjectSkill ps = ProjectSkill.builder().project(project).skill(skill).requiredProficiency(proficiency).build();
        projectSkillRepository.save(ps);
    }
}
