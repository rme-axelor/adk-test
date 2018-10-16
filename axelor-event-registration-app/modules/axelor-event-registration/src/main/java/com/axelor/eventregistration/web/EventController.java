package com.axelor.eventregistration.web;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.mail.MessagingException;

import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.Template;
import com.axelor.apps.message.db.repo.TemplateRepository;
import com.axelor.apps.message.service.MessageService;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.db.Model;
import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.service.EventService;
import com.axelor.exception.AxelorException;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class EventController {

  @Inject MetaFiles metaFiles;

  @Inject EventService eventService;

  @Inject TemplateRepository templateRepo;

  @Inject MetaModelRepository metaModelRepo;

  @Inject TemplateMessageService templateMessageService;

  @Inject MessageService messageService;

  public void computeTotals(ActionRequest request, ActionResponse response) {

    Event event = request.getContext().asType(Event.class);

    response.setValues(eventService.compute(event));
  }

  public void validateDate(ActionRequest request, ActionResponse response) {
    Event event = request.getContext().asType(Event.class);

    LocalDateTime startDate = event.getStartDate();
    LocalDateTime endDate = event.getEndDate();

    if (startDate.isAfter(endDate)) {
      response.addError("endDate", "End date should be higher than or equal to Start Date");
      return;
    }

    LocalDate registrationOpen = event.getRegistrationOpen();
    LocalDate registrationClose = event.getRegistrationClose();

    if (registrationOpen.isAfter(registrationClose)) {
      response.addError(
          "registrationClose",
          "Registration Close Date should be higher than or equal to Registration open Date");
      return;
    }

    if (registrationClose.isAfter(startDate.toLocalDate())
        || registrationClose.isEqual(startDate.toLocalDate())) {
      response.addError(
          "registrationClose", "Registration Close Date should be less than Event Start Date");
    }
  }

  public void sendEmails(ActionRequest request, ActionResponse response) {
    Event event = request.getContext().asType(Event.class);
    MetaModel metaModel =
        metaModelRepo.all().filter("self.fullName = ?", request.getModel()).fetchOne();

    Template template =
        templateRepo.all().filter("self.metaModel = ?", metaModel.getId()).fetchOne();

    List<EventRegistration> eventRegistrationList = event.getEventRegistration();
    for (EventRegistration eventRegistration : eventRegistrationList) {
      if (eventRegistration.getEmail() != null) {

        String content =
            "Event Descriprtion: "
                + event.getDescription()
                + "<br>Registration Date: "
                + eventRegistration.getRegistrationDate()
                + "<br>Event Venue: "
                + event.getVenu()
                + "<br>Amount to Pay: "
                + eventRegistration.getAmount();

        template.setToRecipients(eventRegistration.getEmail());
        template.setSubject(event.getDescription());
        template.setContent(content);

        Model model = metaModel;

        try {
          Message message = templateMessageService.generateMessage(model, template);
          messageService.sendByEmail(message);
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
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        response.setValue("$emailSent", true);
      }
    }
  }
}
