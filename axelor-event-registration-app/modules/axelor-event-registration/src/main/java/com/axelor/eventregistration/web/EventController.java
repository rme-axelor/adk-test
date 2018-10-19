package com.axelor.eventregistration.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.service.EventService;
import com.axelor.eventregistration.translation.ITranslation;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class EventController {

  @Inject EventService eventService;

  public void computeTotals(ActionRequest request, ActionResponse response) {

    Event event = request.getContext().asType(Event.class);

    response.setValues(eventService.compute(event));

    if (event.getRegistrationClose().isBefore(LocalDate.now())
        || event.getRegistrationOpen().isAfter(LocalDate.now())) {
      response.setFlash(I18n.get(ITranslation.REGISTRATIONS_NOT_ALLOWED));
      response.setReadonly("importRegistration", true);
      response.setReadonly("discount", true);
      response.setReadonly("eventRegistration", true);
      return;
    }

    if (!(event.getEventRegistration().isEmpty())) {
      response.setReadonly("discount", true);
    }

    if (event.getCapacity() - event.getTotalEntry() <= 0) {
      response.setReadonly("eventRegistration", true);
    }
  }

  public void validateDate(ActionRequest request, ActionResponse response) {

    Event event = request.getContext().asType(Event.class);

    if (event.getStartDate() == null
        || event.getRegistrationOpen() == null
        || event.getRegistrationClose() == null
        || event.getEndDate() == null) {
      response.setError("Enter All Dates first...!!!");
      return;
    }

    LocalDateTime startDate = event.getStartDate();
    LocalDateTime endDate = event.getEndDate();

    if (startDate.isAfter(endDate)) {
      response.addError("endDate", "End.Date");
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

    if (!registrationClose.isBefore(startDate.toLocalDate())) {
      response.addError(
          "registrationClose", "Registration Close Date should be less than Event Start Date");
      return;
    }

    if (event.getEventRegistration().isEmpty()) {
      response.setAlert(
          "If you want to Add Discount List add it now... Once event registrations will get added.. Discount List will not get change...!");
      return;
    }

    Collections.reverse(event.getEventRegistration());
    LocalDateTime registrationDate =
        event.getEventRegistration().iterator().next().getRegistrationDate();
    if (registrationDate.toLocalDate().isBefore(event.getRegistrationOpen())
        || registrationDate.toLocalDate().isAfter(event.getRegistrationClose())) {
      response.setFlash("Last entered Entry not matches registrations dates of events...");
      response.addError(
          "eventRegistration", "Last entered Entry not matches registrations dates of events...");
    }
  }

  public void sendEmails(ActionRequest request, ActionResponse response) {

    Event event = request.getContext().asType(Event.class);
    eventService.sendEmails(event, request.getModel());
  }
}
