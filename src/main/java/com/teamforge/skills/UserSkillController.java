package com.teamforge.skills;

import com.teamforge.users.User;
import com.teamforge.users.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSkillController {

    private final SkillService skillService;

    @GetMapping("/me/skills")
    public ResponseEntity<List<UserSkillDto>> getMySkills(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(skillService.getUserSkills(user));
    }

    @PostMapping("/me/skills")
    public ResponseEntity<UserSkillDto> addSkillToProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddUserSkillRequest request) {
        return ResponseEntity.ok(skillService.addUserSkill(user, request));
    }

    @DeleteMapping("/me/skills/{userSkillId}")
    public ResponseEntity<Void> removeSkillFromProfile(
            @AuthenticationPrincipal User user,
            @PathVariable Long userSkillId) {
        skillService.removeUserSkill(user, userSkillId);
        return ResponseEntity.noContent().build();
    }

    // Talent Search page: pick a skill from the catalog, see everyone who has it.
    // This is an HR/Manager recruiting tool, not a general employee directory.
    @GetMapping("/search/by-skill")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TalentSearchResultDto>> searchUsersBySkill(@RequestParam Long skillId) {
        return ResponseEntity.ok(skillService.searchUsersBySkill(skillId));
    }

    // Project team assembly: match a project's required tech stack against
    // available (or soon-to-be-available) employees.
    @GetMapping("/search/for-project/{projectId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> searchUsersForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(skillService.searchUsersForProject(projectId));
    }
}
