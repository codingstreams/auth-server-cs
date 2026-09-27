package com.example.authserver.service.mail;

public interface MailService {

  default void sendMail(String recipient) {
    sendMail(LoginAlertDetails.of(recipient));
  }

  default void sendMail(String recipient, String failureReason) {
    sendMail(LoginAlertDetails.of(recipient, failureReason));
  }

  void sendMail(LoginAlertDetails details);
}
