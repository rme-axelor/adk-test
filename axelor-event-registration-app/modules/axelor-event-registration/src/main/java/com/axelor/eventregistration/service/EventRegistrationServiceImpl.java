package com.axelor.eventregistration.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.quartz.xml.ValidationException;

import com.axelor.common.FileUtils;
import com.axelor.data.Listener;
import com.axelor.data.csv.CSVImporter;
import com.axelor.db.Model;
import com.axelor.eventregistration.db.Discount;
import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.eventregistration.db.repo.EventRegistrationRepository;
import com.axelor.eventregistration.db.repo.EventRepository;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.google.common.io.Files;
import com.google.inject.Inject;

public class EventRegistrationServiceImpl implements EventRegistrationService {

  @Inject EventRepository eventRepo;

  @Inject EventRegistrationRepository eventRegistrationRepo;

  @Inject MetaFiles metafiles;

  @Override
  public void importEventRegistrationData(MetaFile file, Event event) throws IOException, Error {

    File tmpDir = null;
    File configXML = null;
    File csvFile = null;
    try {

      tmpDir = Files.createTempDir();
      configXML = new File(tmpDir, "input-config.xml");
      InputStream bindInputStream =
          this.getClass().getResourceAsStream("/import-configs/input-config.xml");

      if (bindInputStream == null) {
        throw new Error("No 'input-config.xml' file found.");
      }

      FileOutputStream outputstream = new FileOutputStream(configXML);
      IOUtils.copy(bindInputStream, outputstream);

      csvFile = new File(tmpDir, "eventRegistration.csv");
      Files.copy(MetaFiles.getPath(file).toFile(), csvFile);

      CSVImporter csvImporter =
          new CSVImporter(tmpDir + "/input-config.xml", tmpDir.getAbsolutePath());

      Map<String, Object> context = new HashMap<>();
      context.put("_event", event);
      csvImporter.setContext(context);

      csvImporter.addListener(
          new Listener() {

            @Override
            public void imported(Integer total, Integer success) {
              System.err.println("Total: " + total + " Success: " + success);
            }

            @Override
            public void imported(Model bean) {}

            @Override
            public void handle(Model bean, Exception e) {}
          });
      csvImporter.run();
    } catch (IOException e) {
      e.printStackTrace();
    }
    configXML.delete();
    csvFile.delete();
    FileUtils.deleteDirectory(tmpDir);
  }

  @Override
  public EventRegistration computeAmount(EventRegistration eventRegistration)
      throws ValidationException {

    Event event = eventRepo.find(eventRegistration.getEvent().getId());

    LocalDate registrationOpen = event.getRegistrationOpen();
    LocalDate registrationClose = event.getRegistrationClose();
    LocalDateTime registrationDate = eventRegistration.getRegistrationDate();

    if (!(registrationDate.toLocalDate().isBefore(registrationOpen))
        && !(registrationDate.toLocalDate().isAfter(registrationClose))) {

      BigDecimal discountamount = BigDecimal.ZERO;

      List<Discount> discountlist = event.getDiscount();
      for (Discount discount : discountlist) {
        Integer beforeDays = discount.getBeforeDays();

        BigDecimal tempDiscount = BigDecimal.ZERO;

        if (!registrationClose.minusDays(beforeDays).isBefore(registrationDate.toLocalDate())) {
          tempDiscount = discountamount;
          discountamount = BigDecimal.ZERO;
          discountamount = discountamount.add(discount.getDiscountAmount());
          if ((registrationClose.minusDays(beforeDays).isEqual(registrationDate.toLocalDate()))) {
            break;
          }
          if (tempDiscount.compareTo(discountamount) == 1) {
            discountamount = tempDiscount;
          }
        }
      }

      eventRegistration.setAmount(event.getEventFees().subtract(discountamount));
      return eventRegistration;
    }
    eventRegistration.setAmount(event.getEventFees());
    return eventRegistration;
  }
}
