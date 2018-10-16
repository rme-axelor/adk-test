package com.axelor.eventregistration.module;

import com.axelor.app.AxelorModule;
import com.axelor.eventregistration.service.EventRegistrationService;
import com.axelor.eventregistration.service.EventRegistrationServiceImpl;
import com.axelor.eventregistration.service.EventService;
import com.axelor.eventregistration.service.EventServiceImpl;

public class EventModule extends AxelorModule {

  @Override
  protected void configure() {

    bind(EventService.class).to(EventServiceImpl.class);
    bind(EventRegistrationService.class).to(EventRegistrationServiceImpl.class);
  }
}
