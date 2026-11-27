package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.TicketResponses.PersonalMealHistory;

public interface ReadPersonalMealHistory {
    PersonalMealHistory personalMealHistory(Long ownerId, int month, int year);
}
