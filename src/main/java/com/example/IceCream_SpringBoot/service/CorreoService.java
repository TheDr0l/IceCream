package com.example.IceCream_SpringBoot.service;

import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;


@Service
public class CorreoService {

    @Autowired
    private JavaMailSender mailSender;


    public void enviarFactura(String destinatario, String asunto, String mensaje, byte[] pdfBytes) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(mensaje);

        // Adjuntar PDF
        helper.addAttachment("Factura.pdf", new ByteArrayResource(pdfBytes));

        mailSender.send(mimeMessage);
    }
}
