package com.axelor.eventregistration.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import com.axelor.apps.message.db.EmailAddress;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.repo.EmailAccountRepository;
import com.axelor.apps.message.db.repo.MessageRepository;
import com.axelor.apps.message.service.MessageService;
import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.db.repo.EventRegistrationRepository;
import com.axelor.eventregistration.db.repo.EventRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;

public class EventServiceImpl implements EventService {

  @Inject EventRegistrationRepository eventRegistrationRepo;

  @Inject EventRepository eventRepo;

  @Inject MessageService messageService;

  @Inject EmailAccountRepository emailaccountRepo;

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

  @Override
  public void sendEmails(Event event, String content, List<EmailAddress> emailAddresses) {

    Message message =
        messageService.createMessage(
            Event.class.getName(),
            event.getId().intValue(),
            event.getDescription(),
            content,
            null,
            null,
            emailAddresses,
            null,
            null,
            null,
            null,
            MessageRepository.MEDIA_TYPE_EMAIL,
            null);

    try {
      messageService.sendByEmail(message);
    } catch (MessagingException
        | IOException
        | AxelorException e) { // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
