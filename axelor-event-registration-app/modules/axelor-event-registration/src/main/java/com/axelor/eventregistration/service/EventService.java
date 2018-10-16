package com.axelor.eventregistration.service;

import java.util.List;
import java.util.Map;

import com.axelor.apps.message.db.EmailAddress;
import com.axelor.eventregistration.db.Event;

public interface EventService {

  public Map<String, Object> compute(Event event);

  public void sendEmails(Event event, String content, List<EmailAddress> emailAddresses);
}
