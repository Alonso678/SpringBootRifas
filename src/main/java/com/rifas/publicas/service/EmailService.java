package com.rifas.publicas.service;

import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend = new Resend(System.getenv("RESEND_API_KEY"));

    public void enviarEmail(String to, String subject, String htmlContent) {
        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .from("notificaciones@fortunarifas.space") // Se configuro el dominio comprado en namecheap.com
                .to(to)
                .subject(subject)
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(sendEmailRequest);
        } catch (Exception e) {
            System.err.println("Error al enviar email con Resend: " + e.getMessage());
        }
    }
}