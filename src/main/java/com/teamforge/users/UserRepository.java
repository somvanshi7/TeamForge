package com.teamforge.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE EXISTS (SELECT 1 FROM UserSkill us WHERE us.user = u AND LOWER(us.skill.name) = LOWER(:skillName))")
    List<User> findUsersBySkillName(@Param("skillName") String skillName);
}
