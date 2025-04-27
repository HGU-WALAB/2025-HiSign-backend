package com.example.backend.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {

    private boolean secure;

    private String sameSite;

    private long accessTokenMaxAge;

    private long refreshTokenMaxAge;

    private long signerTokenMaxAge;
}