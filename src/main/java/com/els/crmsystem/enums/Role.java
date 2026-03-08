package com.els.crmsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    // 1. CONSTANT | 2. English | 3. Ukrainian | 4. Description

    ADMIN(
            "Administrator",
            "Адміністратор",
            "Has full access to code, DB, and logs (Tech Lead)"
    ),

    DIRECTOR(
            "Director",
            "Директор",
            "Can manage users and delete projects"
    ),

    MANAGER(
            "Manager",
            "Менеджер",
            "Can manage projects and tasks, but cannot delete users"
    ),

    INSTALLER(
            "Installer",
            "Монтажник",
            "Can view and update tasks/project assigned to them"
    ),

    GUEST(
            "Guest",
            "Гість",
            "Default role for new registrations."
    );

    private final String englishName;
    private final String ukrainianName;
    private final String description;
}
