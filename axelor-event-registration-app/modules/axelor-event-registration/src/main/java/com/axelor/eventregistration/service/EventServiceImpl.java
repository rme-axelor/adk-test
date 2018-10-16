package com.axelor.eventregistration.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.db.repo.EventRegistrationRepository;
import com.axelor.eventregistration.db.repo.EventRepository;
import com.google.inject.Inject;

public class EventServiceImpl implements EventService {

  @Inject EventRegistrationRepository eventRegistrationRepo;

  @Inject EventRepository eventRepo;

  @Override
  public Map<String, Object> compute(Event event) {

    Map<String, Object> computeTotal = new HashMap<>();

    Long totalEntry = eventRegistrationRepo.all().filter("self.event = ?", event.getId()).count();

    List<EventRegistration> eventRegistrations =
        eventRegistrationRepo.all().filter("self.event = ?", event.getId()).fetch();

    BigDecimal totalAmount = BigDecimal.ZERO;

    for (EventRegistration eventRegistration : eventRegistrations) {
      totalAmount = totalAmount.add(eventRegistration.getAmount());
    }

    System.err.println(event.getEventFees());
    System.err.println(BigDecimal.valueOf(totalEntry));
    System.err.println(totalAmount);

    BigDecimal totalD = event.getEventFees().multiply(BigDecimal.valueOf(totalEntry));
    totalD = totalD.subtract(totalAmount);

    computeTotal.put("eventRegistration", eventRegistrations);
    computeTotal.put("amountCollected", totalAmount);
    computeTotal.put("totalEntry", totalEntry);
    computeTotal.put("totalDiscount", totalD);
    return computeTotal;
  }
}
