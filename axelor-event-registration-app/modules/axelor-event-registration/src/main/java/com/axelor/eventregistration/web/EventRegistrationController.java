package com.axelor.eventregistration.web;

import java.util.LinkedHashMap;

import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.db.repo.EventRepository;
import com.axelor.eventregistration.service.EventRegistrationService;
import com.axelor.eventregistration.translation.ITranslation;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class EventRegistrationController {

  @Inject EventRepository eventRepo;

  @Inject EventRegistrationService eventRegistrationService;

  public void validateEvent(ActionRequest request, ActionResponse response) {

    EventRegistration eventRegistration = request.getContext().asType(EventRegistration.class);

    if (eventRegistration.getEvent() == null) {
      return;
    }

    response.setValues(eventRegistrationService.validateEvent(eventRegistration));
    response.setReadonly("event", true);
  }

  public void validateDate(ActionRequest request, ActionResponse response) {
    EventRegistration eventRegistration = request.getContext().asType(EventRegistration.class);
    Event event = eventRepo.find(eventRegistration.getEvent().getId());

    if ((event.getCapacity() - event.getTotalEntry()) <= 0) {
      response.addError("event", I18n.get(ITranslation.REGISTRATIONS_FULL));
      return;
    }

    if (eventRegistration
        .getRegistrationDate()
        .toLocalDate()
        .isAfter(event.getRegistrationClose())) {
      response.addError(
          "registrationDate",
          "Registration Date is Not Between Registration Dates of this Event... Try again Later!");
    }
  }

  public void validateEmail(ActionRequest request, ActionResponse response) {

    EventRegistration eventRegistration = request.getContext().asType(EventRegistration.class);

    String email = eventRegistration.getEmail();
    if (email == null) {
      response.addError("email", "Enter Email Id");
      return;
    }

    if (!email.matches("[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}")) {
      response.addError("email", "Enter Valid Email Id");
      return;
    }
  }

  public void setEvent(ActionRequest request, ActionResponse response) {
    if (request.getContext().getParent() == null) {
      return;
    }

    Event event1 = request.getContext().getParent().asType(Event.class);

    if (event1.getId() == null) {
      response.setError("Please save event First!!!");
      return;
    }

    Event event = eventRepo.find(event1.getId());
    response.setValue("event", event);
    response.setHidden("event", true);
  }

  public void importRegistrations(ActionRequest request, ActionResponse response) {

    @SuppressWarnings("unchecked")
    LinkedHashMap<String, Object> map =
        (LinkedHashMap<String, Object>) request.getContext().get("metaFile");
    MetaFile dataFile =
        Beans.get(MetaFileRepository.class).find(((Integer) map.get("id")).longValue());

    if (!dataFile.getFileType().equals("text/csv")) {
      response.setError("Only .csv formats are Accepted!!!");
      return;
    }

    Long id = Long.valueOf(request.getContext().get("_id").toString());
    eventRegistrationService.importEventRegistrationData(dataFile, eventRepo.find(id));
  }
}
