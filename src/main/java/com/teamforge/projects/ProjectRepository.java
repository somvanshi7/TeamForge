package com.teamforge.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCase(String title);
}
