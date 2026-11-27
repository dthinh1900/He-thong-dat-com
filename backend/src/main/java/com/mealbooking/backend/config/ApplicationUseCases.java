package com.mealbooking.backend.config;

import com.mealbooking.backend.application.port.in.AccountProfiles;
import com.mealbooking.backend.application.port.in.ReadPersonalMealHistory;
import com.mealbooking.backend.application.port.in.SignIn;
import com.mealbooking.backend.application.port.out.AccountAuthenticator;
import com.mealbooking.backend.application.port.out.MealHistoryReader;
import com.mealbooking.backend.application.port.out.SessionTokenIssuer;
import com.mealbooking.backend.application.usecase.PersonalMealHistoryUseCase;
import com.mealbooking.backend.application.usecase.SignInUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationUseCases {
    @Bean
    SignIn signIn(AccountAuthenticator authenticator, SessionTokenIssuer tokens, AccountProfiles profiles) {
        return new SignInUseCase(authenticator, tokens, profiles);
    }

    @Bean
    ReadPersonalMealHistory personalMealHistory(MealHistoryReader reader) {
        return new PersonalMealHistoryUseCase(reader);
    }
}
