package ru.college.carmarketplace.service.impl;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import ru.college.carmarketplace.service.EmailService;

public class EmailServiceImpl implements EmailService {
    @Override
    public void sendEmailConfirmation(String emailToSend, String confirmationCode) {
        Email email = EmailBuilder
                .startingBlank()
                .from("CarEmailConfirmation", "hairydonut766@gmail.com")
                .to(emailToSend)
                .withSubject("Код подтверждения")
                .withPlainText("Здравствуйте, ваш код подтверждения аккаунта " + confirmationCode).buildEmail();
        try (Mailer mailer = MailerBuilder
                .withSMTPServer("smtp.gmail.com", 587, "hairydonut766@gmail.com", "jyms sami owwx zyqh")
                .withTransportStrategy(TransportStrategy.SMTP_TLS).buildMailer()) {
            mailer.sendMail(email);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
