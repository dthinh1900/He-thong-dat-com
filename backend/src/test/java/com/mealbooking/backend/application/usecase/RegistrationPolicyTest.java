package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.model.AdminResponses.Settings;
import java.time.*;
import org.junit.jupiter.api.Test;
import com.mealbooking.backend.application.port.out.RegistrationSettingsStore;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RegistrationPolicyTest {
    private final ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");
    private final Settings settings=new Settings(LocalTime.of(14,45),LocalTime.of(15,30));
    private RegistrationPolicy at(int hour,int minute){
        Clock clock=Clock.fixed(ZonedDateTime.of(2026,11,18,hour,minute,0,0,zone).toInstant(),zone);
        return new RegistrationPolicy(mock(RegistrationSettingsStore.class),clock,true);
    }
    @Test void tomorrowCutoffIsInclusiveAndLaterWithdrawalDeadlineCannotExtendIt(){
        LocalDate tomorrow=LocalDate.of(2026,11,19);
        assertNull(at(14,44).lockReason(tomorrow,false,settings));
        assertNotNull(at(14,45).lockReason(tomorrow,false,settings));
        assertNull(at(14,44).lockReason(tomorrow,true,settings));
        assertNotNull(at(14,45).lockReason(tomorrow,true,settings));
        var earlier = new Settings(LocalTime.of(14,45),LocalTime.of(13,30));
        assertNotNull(at(13,30).lockReason(tomorrow,true,earlier));
        assertNull(at(13,30).lockReason(tomorrow,false,earlier));
        assertNotNull(at(15,30).lockReason(tomorrow,true,settings));
    }
    @Test void pastTodayAndWeekendsAreLockedButLaterWeekdaysAreOpen(){
        var policy=at(18,0);
        assertNotNull(policy.lockReason(LocalDate.of(2026,11,17),false,settings));
        assertNotNull(policy.lockReason(LocalDate.of(2026,11,18),false,settings));
        assertNotNull(policy.lockReason(LocalDate.of(2026,11,21),false,settings));
        assertNull(policy.lockReason(LocalDate.of(2026,11,20),false,settings));
    }

    @Test void cutoffUsesCalendarDayAcrossYearAndAdminCanStillCancelTomorrow() {
        var eve = ZonedDateTime.of(2026, 12, 31, 14, 45, 0, 0, zone);
        var policy = new RegistrationPolicy(mock(RegistrationSettingsStore.class), Clock.fixed(eve.toInstant(), zone), true);
        var january = LocalDate.of(2027, 1, 1);
        assertNotNull(policy.lockReason(january, false, settings));
        assertNull(policy.adminCancellationLock(january));
        assertNotNull(policy.adminCancellationLock(eve.toLocalDate()));
    }
}
