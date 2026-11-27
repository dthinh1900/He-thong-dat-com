package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.model.AuthenticatedAccount;

public interface AccountAuthenticator {
    AuthenticatedAccount verify(String username, String password);
}
