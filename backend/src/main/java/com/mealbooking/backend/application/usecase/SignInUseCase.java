package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.in.AccountProfiles;
import com.mealbooking.backend.application.port.in.SignIn;
import com.mealbooking.backend.application.port.out.AccountAuthenticator;
import com.mealbooking.backend.application.port.out.SessionTokenIssuer;
import com.mealbooking.backend.application.model.LoginResponse;

public final class SignInUseCase implements SignIn {
    private final AccountAuthenticator authenticator;
    private final SessionTokenIssuer tokens;
    private final AccountProfiles profiles;

    public SignInUseCase(AccountAuthenticator authenticator, SessionTokenIssuer tokens, AccountProfiles profiles) {
        this.authenticator = authenticator;
        this.tokens = tokens;
        this.profiles = profiles;
    }

    @Override
    public LoginResponse login(String username, String password) {
        var account = authenticator.verify(username.trim(), password);
        String token = tokens.issue(account);
        return new LoginResponse(token, "Bearer", profiles.getProfile(account.id()));
    }
}
