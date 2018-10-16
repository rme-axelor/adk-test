package com.axelor.eventregistration.web;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.service.EventService;
import com.axelor.meta.MetaFiles;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class EventController {

  @Inject MetaFiles metaFiles;

  @Inject EventService eventService;

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

  public void openEventRegistrationForm(ActionRequest request, ActionResponse response) {
    request.getContext().put("_event", request.getContext().asType(Event.class));
  }
}
