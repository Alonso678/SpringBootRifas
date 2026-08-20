package com.rifas.publicas.service;

import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    public void enviarEmail(String to, String subject, String htmlContent) {
        // Inicializamos Resend aquí adentro usando la llave inyectada
        Resend resend = new Resend(resendApiKey);

        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .from("notificaciones@fortunarifas.space")
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