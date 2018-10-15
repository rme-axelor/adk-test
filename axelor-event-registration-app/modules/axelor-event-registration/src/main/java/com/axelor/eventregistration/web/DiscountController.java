package com.axelor.eventregistration.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.axelor.eventregistration.db.Discount;
import com.axelor.eventregistration.db.Event;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class DiscountController {

  public void compute(ActionRequest request, ActionResponse response) {
    Discount discount = request.getContext().asType(Discount.class);
    Event event = request.getContext().getParent().asType(Event.class);

    if (event.getEventFees() == null) {
      response.setError("Enter Event Fees First!!!!");
      return;
    }
    BigDecimal eventFees = event.getEventFees();
    BigDecimal dicount = discount.getDiscountPercent();
    BigDecimal hundred = BigDecimal.valueOf(100l);
    response.setValue("discountAmount", dicount.multiply(eventFees).divide(hundred));
  }

  public void computeBeforeDays(ActionRequest request, ActionResponse response) {

    Discount discount = request.getContext().asType(Discount.class);
    Event event = request.getContext().getParent().asType(Event.class);

    Integer beforeDays = discount.getBeforeDays();

    LocalDate registrationOpen = event.getRegistrationOpen();
    LocalDate registrationClose = event.getRegistrationClose();

    if (registrationClose == null || registrationOpen == null) {
      response.addError("beforeDays", "Please Enter Registration dates of Event!!!");
      return;
    }
    LocalDate newDate = registrationClose.minusDays(beforeDays);
    if (!newDate.isAfter(registrationOpen)) {
      response.addError("beforeDays", "Before Days Exceeds gap of Registrations Dates!!!");
    }
  }
}
