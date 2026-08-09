package com.teamforge.skills;

import org.springframework.data.jpa.repository.JpaRepository;
import com.teamforge.users.User;
import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUser(User user);
    List<UserSkill> findBySkill(Skill skill);
    boolean existsByUserAndSkill(User user, Skill skill);
}
