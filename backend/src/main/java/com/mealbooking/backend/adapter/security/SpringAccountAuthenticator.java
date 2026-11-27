package com.mealbooking.backend.adapter.security;

import com.mealbooking.backend.application.model.AuthenticatedAccount;
import com.mealbooking.backend.application.port.out.AccountAuthenticator;
import com.mealbooking.backend.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SpringAccountAuthenticator implements AccountAuthenticator {
    private final AuthenticationManager authenticationManager;

    public SpringAccountAuthenticator(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthenticatedAccount verify(String username, String password) {
        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));
        var principal = (UserPrincipal) authentication.getPrincipal();
        return new AuthenticatedAccount(principal.user().getId(), principal.getUsername(), principal.user().getRole());
    }
}
