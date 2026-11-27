package com.mealbooking.backend.config;

import com.mealbooking.backend.application.port.in.AccountProfiles;
import com.mealbooking.backend.application.port.in.AdminMeals;
import com.mealbooking.backend.application.port.in.EmployeeAdministration;
import com.mealbooking.backend.application.port.in.MealRegistrations;
import com.mealbooking.backend.application.port.in.MealReports;
import com.mealbooking.backend.application.port.in.TicketOperations;
import com.mealbooking.backend.application.port.in.TicketQueries;
import com.mealbooking.backend.application.port.in.WeeklyMenus;
import com.mealbooking.backend.application.port.out.*;
import com.mealbooking.backend.application.usecase.*;
import java.time.Clock;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration(proxyBeanMethods = false)
class MealBookingUseCases {
    @Bean
    PasswordHasher passwordHasher(PasswordEncoder passwordEncoder) {
        return passwordEncoder::encode;
    }

    @Bean
    RegistrationPolicy registrationPolicy(RegistrationSettingsStore store, Clock clock,
            @Value("${app.registration.exclude-weekends:true}") boolean excludeWeekends) {
        return new RegistrationPolicy(store, clock, excludeWeekends);
    }

    @Bean
    AccountProfiles accountProfiles(UserStore users, PasswordHasher passwords, PlatformTransactionManager manager) {
        return transactional(AccountProfiles.class, new UserService(users, passwords), manager, false, "getProfile");
    }

    @Bean
    MealRegistrations mealRegistrations(MealRegistrationStore registrations, MealTicketStore tickets, MealPriceStore prices,
            AuditLogStore audit, UserStore users, Clock clock, RegistrationPolicy policy, PlatformTransactionManager manager) {
        return transactional(MealRegistrations.class, new MealRegistrationService(registrations, tickets, prices,
            audit, users, clock, policy), manager, false, "getMonth");
    }

    @Bean
    TicketOperations ticketOperations(TicketPurchaseStore purchases, TicketExchangeStore offers,
            TicketExchangeTransactionStore transfers, MealTicketStore tickets, MealRegistrationStore registrations, MealPriceStore prices,
            UserStore users, AuditLogStore audit, Clock clock, PlatformTransactionManager manager) {
        return transactional(TicketOperations.class, new TicketOperationsService(purchases, offers, transfers,
            tickets, registrations, prices, users, audit, clock), manager, false, "purchases", "quotePurchase", "board", "transactions");
    }

    @Bean
    TicketQueries ticketQueries(TicketDirectory directory, MealTicketStore tickets, UserStore users,
            RegistrationPolicy policy, PlatformTransactionManager manager) {
        return transactional(TicketQueries.class, new TicketLookupService(directory, tickets, users, policy),
            manager, false, "*");
    }

    @Bean
    EmployeeAdministration employeeAdministration(UserStore users, DepartmentStore departments, PasswordHasher passwords,
            PeopleDirectory directory, PlatformTransactionManager manager) {
        return transactional(EmployeeAdministration.class, new AdminPeopleService(users, departments, passwords, directory),
            manager, false, "departments", "employees", "employee");
    }

    @Bean
    AdminMeals adminMeals(AdminMealData data, Clock clock, RegistrationPolicy policy, MealRegistrations registrations,
            MealPriceStore prices, UserStore users, PlatformTransactionManager manager) {
        return transactional(AdminMeals.class, new AdminMealService(data, clock, policy, registrations, prices, users),
            manager, false, "overview", "kitchenCounts", "registrations", "prices");
    }

    @Bean
    WeeklyMenus weeklyMenus(MenuImages images, MenuImageInspector inspector, Clock clock, PlatformTransactionManager manager) {
        return transactional(WeeklyMenus.class, new AdminMenuService(images, inspector, clock), manager, false, "get", "image");
    }

    @Bean
    MealReports mealReports(MealReportReader reader, PlatformTransactionManager manager) {
        return transactional(MealReports.class, new AdminReportService(reader), manager, true, "*");
    }

    private static <T> T transactional(Class<T> port, T implementation, PlatformTransactionManager manager,
            boolean consistentRead, String... readMethods) {
        var attributes = new NameMatchTransactionAttributeSource();
        attributes.addTransactionalMethod("*", new RuleBasedTransactionAttribute());
        var read = new RuleBasedTransactionAttribute();
        read.setReadOnly(true);
        if (consistentRead) read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        for (String method : readMethods) attributes.addTransactionalMethod(method, read);

        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(attributes);

        var proxy = new ProxyFactory();
        proxy.setTarget(implementation);
        proxy.setInterfaces(port);
        proxy.addAdvice(interceptor);
        return port.cast(proxy.getProxy());
    }
}
