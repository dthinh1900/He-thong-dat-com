package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.in.RegistrationSettings;
import com.mealbooking.backend.application.port.out.RegistrationSettingsStore;
import com.mealbooking.backend.application.model.AdminResponses.Settings;
import com.mealbooking.backend.domain.model.MealTicket;
import com.mealbooking.backend.domain.registration.RegistrationRules;
import java.time.*;

public class RegistrationPolicy implements RegistrationSettings {
    private final RegistrationSettingsStore store;
    private final Clock clock;
    private final boolean excludeWeekends;
    public RegistrationPolicy(RegistrationSettingsStore store, Clock clock, boolean excludeWeekends) {
        this.store=store; this.clock=clock; this.excludeWeekends=excludeWeekends;
    }
    @Override
    public Settings settings() { return store.read(); }

    @Override
    public void update(Settings settings) { store.save(settings); }
    public static LocalTime withdrawalDeadline(Settings settings) {
        return settings.cancelDeadline().isBefore(settings.cutoff()) ? settings.cancelDeadline() : settings.cutoff();
    }

    public String lockReason(LocalDate date, boolean withdrawal, Settings settings) {
        LocalTime deadline = withdrawal ? withdrawalDeadline(settings) : settings.cutoff();
        return RegistrationRules.lockReason(date, LocalDateTime.now(clock), deadline, excludeWeekends);
    }

    public boolean canWithdraw(MealTicket ticket, Settings settings) {
        return ticket.getRegistration() != null
            && ticket.getOwner().getId().equals(ticket.getRegistration().getUser().getId())
            && lockReason(ticket.getMealDate(), true, settings) == null;
    }

    public String adminCancellationLock(LocalDate date) {
        return RegistrationRules.adminCancellationLock(date, LocalDate.now(clock));
    }
}
