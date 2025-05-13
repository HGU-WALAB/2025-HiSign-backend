package com.example.backend.auth.config;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class AdminList {
    public static final List<String> ADMIN_UNIQUE_IDS = Arrays.asList(
            "50666",
            "50654",
            "21700214",
            "22200103",
            "22200256"
    );
}