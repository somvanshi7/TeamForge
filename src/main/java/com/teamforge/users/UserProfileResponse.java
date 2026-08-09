package com.teamforge.users;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String department,
        String designation,
        String role,
        java.time.LocalDate availableFrom
) {}
