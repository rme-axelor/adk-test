package com.axelor.eventregistration.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.validation.ValidationException;

import com.axelor.data.Listener;
import com.axelor.data.csv.CSVImporter;
import com.axelor.db.Model;
import com.axelor.eventregistration.db.Discount;
import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.db.repo.EventRegistrationRepository;
import com.axelor.eventregistration.db.repo.EventRepository;
import com.axelor.eventregistration.translation.ITranslation;
import com.axelor.i18n.I18n;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.google.common.io.Files;
import com.google.inject.Inject;

public class EventRegistrationServiceImpl implements EventRegistrationService {

  @Inject EventRepository eventRepo;

  @Inject EventRegistrationRepository eventRegistrationRepo;

  @Override
  public void importEventRegistrationData(MetaFile file, Event event) {

    try {
      File tmpDir =
          new File(
              "/home/axelor/rcm/Workspace/axelor-event-registration-app/modules/axelor-event-registration/src/main/resources/data-demo/input");
      File fileOutputStream = new File(tmpDir, "eventRegistration.csv");
      File inputFile = MetaFiles.getPath(file).toFile();
      Files.copy(inputFile, fileOutputStream);

      CSVImporter csvImporter =
          new CSVImporter(
              "/home/axelor/rcm/Workspace/axelor-event-registration-app/modules/axelor-event-registration/src/main/resources/data-demo/input-config.xml",
              tmpDir.getAbsolutePath());
      csvImporter.addListener(
          new Listener() {

            @Override
            public void imported(Integer total, Integer success) {
              System.out.println("Total: " + total + " Success: " + success);
            }

            @Override
            public void imported(Model bean) {
              EventRegistration eventRegistration = (EventRegistration) bean;
              eventRegistration.setEvent(event);
              //              Object object = validateEvent(eventRegistration).get("amount");
              //
              // eventRegistration.setAmount(BigDecimal.valueOf(Long.valueOf(object.toString())));
              eventRegistration = validateEvent(eventRegistration);
              eventRegistrationRepo.save(eventRegistration);
            }

            @Override
            public void handle(Model bean, Exception e) {
              e.printStackTrace();
            }
          });
      csvImporter.run();

    } catch (IOException e) { // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public EventRegistration validateEvent(EventRegistration eventRegistration) {

    //    Map<String, Object> eventRegistrationMap = new HashMap<>();
    System.err.println(eventRegistration.getEvent().getId());
    Event event = eventRepo.find(eventRegistration.getEvent().getId());

    if ((event.getCapacity() - event.getTotalEntry()) <= 0) {
      throw new ValidationException(I18n.get(ITranslation.REGISTRATIONS_FULL));
    }

    LocalDate registrationOpen = event.getRegistrationOpen();
    LocalDate registrationClose = event.getRegistrationClose();
    LocalDateTime registrationDate = eventRegistration.getRegistrationDate();

    if ((registrationDate.toLocalDate().isAfter(registrationOpen)
            || registrationDate.toLocalDate().isEqual(registrationOpen))
        && (registrationDate.toLocalDate().isBefore(registrationClose)
            || registrationDate.toLocalDate().isEqual(registrationClose))) {

      BigDecimal discountamount = BigDecimal.ZERO;

      List<Discount> discountlist = event.getDiscount();
      for (Discount discount : discountlist) {
        Integer beforeDays = discount.getBeforeDays();

        if ((registrationClose.minusDays(beforeDays).isAfter(registrationDate.toLocalDate()))
            || (registrationClose.minusDays(beforeDays).isEqual(registrationDate.toLocalDate()))) {
          discountamount = BigDecimal.ZERO;
          discountamount = discountamount.add(discount.getDiscountAmount());
        }
      }

      eventRegistration.setAmount(event.getEventFees().subtract(discountamount));
      //      eventRegistrationMap.put("amount", event.getEventFees().subtract(discountamount));
      return eventRegistration;
    }
    //    eventRegistrationMap.put("amount", event.getEventFees());
    eventRegistration.setAmount(event.getEventFees());
    return eventRegistration;
  }
}
