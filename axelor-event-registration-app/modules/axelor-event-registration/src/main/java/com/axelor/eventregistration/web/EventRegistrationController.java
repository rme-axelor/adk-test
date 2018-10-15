package com.axelor.eventregistration.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.axelor.eventregistration.db.Discount;
import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.db.repo.EventRepository;
import com.axelor.eventregistration.translation.ITranslation;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class EventRegistrationController {

  @Inject EventRepository eventRepo;

  public void validateEvent(ActionRequest request, ActionResponse response) {

    EventRegistration eventRegistration = request.getContext().asType(EventRegistration.class);

    Event event = eventRepo.find(eventRegistration.getEvent().getId());

    if ((event.getCapacity() - event.getTotalEntry()) <= 0) {
      response.addError("event", I18n.get(ITranslation.REGISTRATIONS_FULL));
      return;
    }

    LocalDate registrationOpen = event.getRegistrationOpen();
    LocalDate registrationClose = event.getRegistrationClose();
    LocalDateTime registrationDate = eventRegistration.getRegistrationDate();

    if (registrationDate.toLocalDate().isAfter(registrationOpen)
        && registrationDate.toLocalDate().isBefore(registrationClose)) {

      BigDecimal discountamount = BigDecimal.ZERO;

      List<Discount> discountlist = event.getDiscount();
      for (Discount discount : discountlist) {
        Integer beforeDays = discount.getBeforeDays();

        if ((registrationClose.minusDays(beforeDays).isAfter(registrationDate.toLocalDate()))
            || (registrationClose.minusDays(beforeDays).isEqual(registrationDate.toLocalDate()))) {
          discountamount = discountamount.add(discount.getDiscountAmount());
          break;
        }
      }

      response.setValue("amount", event.getEventFees().subtract(discountamount));
      return;
    }

    response.addError("registrationDate", I18n.get(ITranslation.REGISTRATION_DATE_ERROR));
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

    Event event = request.getContext().getParent().asType(Event.class);

    response.setValue("amount", event.getEventFees());
  }
}
