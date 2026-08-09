package com.teamforge.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import com.teamforge.users.User;
import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByUser(User user);
    List<ProjectMember> findByProject(Project project);
    boolean existsByUserAndProject(User user, Project project);
}
