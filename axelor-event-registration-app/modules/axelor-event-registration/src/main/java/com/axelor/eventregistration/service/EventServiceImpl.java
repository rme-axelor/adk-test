package com.axelor.eventregistration.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.validation.ValidationException;

import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.Template;
import com.axelor.apps.message.db.repo.TemplateRepository;
import com.axelor.apps.message.service.MessageService;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.db.repo.EventRegistrationRepository;
import com.axelor.eventregistration.db.repo.EventRepository;
import com.axelor.exception.AxelorException;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class EventServiceImpl implements EventService {

  @Inject EventRegistrationRepository eventRegistrationRepo;

  @Inject EventRepository eventRepo;

  @Inject MessageService messageService;

  @Inject MetaFiles metaFiles;

  @Inject TemplateRepository templateRepo;

  @Inject MetaModelRepository metaModelRepo;

  @Inject TemplateMessageService templateMessageService;

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

    BigDecimal totalD = event.getEventFees().multiply(BigDecimal.valueOf(totalEntry));
    totalD = totalD.subtract(totalAmount);

    computeTotal.put("amountCollected", totalAmount);
    computeTotal.put("totalEntry", totalEntry);
    computeTotal.put("totalDiscount", totalD);
    return computeTotal;
  }

  @Override
  @Transactional
  public void sendEmails(Event event, String model) {
    MetaModel metaModel = metaModelRepo.all().filter("self.fullName = ?", model).fetchOne();

    Template template =
        templateRepo.all().filter("self.metaModel = ?", metaModel.getId()).fetchOne();
    if (template == null) {
      throw new ValidationException("Template Missing... Please Create template First");
    }

    List<EventRegistration> eventRegistrationList = event.getEventRegistration();
    for (EventRegistration eventRegistration : eventRegistrationList) {
      if (eventRegistration.getEmail() != null) {

        String content =
            "<b>Event Reference: </b>"
                + event.getReference()
                + "<b>Event Descriprtion: </b>"
                + event.getDescription()
                + "<br><br><b>Participant Name: </b>"
                + eventRegistration.getName()
                + "<br><b>Registration Date: </b>"
                + eventRegistration.getRegistrationDate()
                + "<br><br><b>Event Venue: </b>"
                + event.getVenu().getFullname()
                + "<br><b>Amount to Pay: </b>"
                + eventRegistration.getAmount();

        template.setName(event.getDescription());
        template.setToRecipients(eventRegistration.getEmail());
        template.setSubject(event.getDescription());
        template.setContent(content);

        try {
          Message message = templateMessageService.generateMessage(event, template);
          messageService.sendByEmail(message);
          eventRegistration.setEmailSent(true);
          eventRegistrationRepo.save(eventRegistration);
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (AxelorException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (MessagingException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
