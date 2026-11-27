package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.model.AuthenticatedAccount;

public interface SessionTokenIssuer {
    String issue(AuthenticatedAccount account);
}
