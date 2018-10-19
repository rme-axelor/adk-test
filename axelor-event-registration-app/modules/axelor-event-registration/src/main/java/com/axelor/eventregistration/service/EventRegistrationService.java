package com.axelor.eventregistration.service;

import java.io.IOException;

import org.quartz.xml.ValidationException;

import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.meta.db.MetaFile;

public interface EventRegistrationService {

  public void importEventRegistrationData(MetaFile file, Event event) throws IOException;

  public EventRegistration computeAmount(EventRegistration eventRegistration)
      throws ValidationException;
}
