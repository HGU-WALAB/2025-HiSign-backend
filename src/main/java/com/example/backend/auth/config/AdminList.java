package com.example.backend.auth.config;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class AdminList {
    public static final List<String> ADMIN_UNIQUE_IDS = Arrays.asList(
            "50666",
            "50654",
            "22200256"
    );
}