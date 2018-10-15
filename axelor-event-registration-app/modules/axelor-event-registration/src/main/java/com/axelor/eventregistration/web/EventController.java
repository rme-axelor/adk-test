package com.axelor.eventregistration.web;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import com.axelor.data.Listener;
import com.axelor.data.csv.CSVImporter;
import com.axelor.db.Model;
import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.db.repo.EventRegistrationRepository;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.io.Files;
import com.google.inject.Inject;

public class EventController {

  @Inject EventRegistrationRepository eventRegistrationRepo;

  @Inject MetaFiles metaFiles;

  public void ImportRegistrations(ActionRequest request, ActionResponse response) {

    LinkedHashMap<String, Object> map =
        (LinkedHashMap<String, Object>) request.getContext().get("metaFile");
    MetaFile dataFile =
        Beans.get(MetaFileRepository.class).find(((Integer) map.get("id")).longValue());

    String fileName = dataFile.getFileName();
    fileName = fileName.substring(fileName.length() - 3, fileName.length());

    if (!fileName.equals("csv")) {

      response.setError("Only .csv formats are Accepted!!!");
      return;
    }

    try {
      File tmpDir = new File("test");
      File fileOutputStream = new File(tmpDir, "eventRegistration.csv");
      File inputFile = MetaFiles.getPath(dataFile).toFile();
      Files.copy(inputFile, fileOutputStream);

      CSVImporter csvImporter = new CSVImporter("test/csvER-Config.xml", tmpDir.getAbsolutePath());
      csvImporter.addListener(
          new Listener() {

            @Override
            public void imported(Integer total, Integer success) {
              System.out.println("Total: " + total + " Success: " + success);
            }

            @Override
            public void imported(Model bean) {}

            @Override
            public void handle(Model bean, Exception e) {
              e.printStackTrace();
            }
          });
      csvImporter.run();
      response.setFlash("Data Imported SuccessFully :)");
    } catch (IOException e) { // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void computeTotals(ActionRequest request, ActionResponse response) {

    Event event = request.getContext().asType(Event.class);
    Long totalEntry = eventRegistrationRepo.all().filter("self.event = ?", event.getId()).count();

    List<EventRegistration> eventRegistrations =
        eventRegistrationRepo.all().filter("self.event = ?", event.getId()).fetch();

    BigDecimal totalAmount = BigDecimal.ZERO;

    for (EventRegistration eventRegistration : eventRegistrations) {
      totalAmount = totalAmount.add(eventRegistration.getAmount());
    }

    response.setValue("eventRegistration", eventRegistrations);
    response.setValue("amountCollected", totalAmount);
    response.setValue("totalEntry", totalEntry);
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
    response.setView(
        ActionView.define("Event Registration")
            .model(EventRegistration.class.getName())
            .add("form", "event-registration-form")
            .context("_event", request.getContext().asType(Event.class))
            .map());
  }
}
