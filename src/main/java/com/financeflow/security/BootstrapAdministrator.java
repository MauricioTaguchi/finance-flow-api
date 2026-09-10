package com.financeflow.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdministrator implements ApplicationRunner {
    private final PrincipalAdministration administration;
    private final String username;
    private final String token;

    public BootstrapAdministrator(PrincipalAdministration administration,
            @Value("${app.bootstrap.admin-username}") String username,
            @Value("${app.bootstrap.admin-token}") String token) {
        this.administration = administration; this.username = username; this.token = token;
    }

    @Override public void run(ApplicationArguments arguments) { administration.bootstrap(username, token); }
}
