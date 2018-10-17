package com.axelor.eventregistration.service;

import java.util.Map;

import com.axelor.eventregistration.db.Event;

public interface EventService {

  public Map<String, Object> compute(Event event);

  public void sendEmails(Event event, String model);
}
