package com.teamforge.skills;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ResponseEntity<List<SkillDto>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    // HR (ADMIN) and Managers can add a skill missing from the catalog -
    // this immediately reflects in the Skill Gap metric for recruitment planning.
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SkillDto> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        return ResponseEntity.ok(skillService.createSkill(request));
    }
}
