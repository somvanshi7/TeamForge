package com.teamforge.users;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal User user) {
        return new UserProfileResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getDepartment(), user.getDesignation(), user.getRole().name(), null
        );
    }
}
