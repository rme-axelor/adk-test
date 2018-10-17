package com.axelor.eventregistration.service;

import com.axelor.eventregistration.db.Event;
import com.axelor.eventregistration.db.EventRegistration;
import com.axelor.meta.db.MetaFile;

public interface EventRegistrationService {

  public void importEventRegistrationData(MetaFile file, Event event);

  public EventRegistration validateEvent(EventRegistration eventRegistration);
}
