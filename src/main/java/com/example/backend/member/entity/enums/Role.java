package com.example.backend.member.entity.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Role {
    USER(0, "ROLE_USER"),
    ADMIN(1, "ROLE_ADMIN");

    private final int level;
    @Getter
    private final String roleName;

    Role(int level, String roleName) {
        this.level = level;
        this.roleName = roleName;
    }

    public static Role fromLevel(int level) {
        return Arrays.stream(Role.values())
                .filter(role -> role.level == level)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("잘못된 권한 레벨: " + level));
    }
}
