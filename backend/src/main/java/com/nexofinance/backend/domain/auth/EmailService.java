package com.nexofinance.backend.domain.auth;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username:nao-responda@nexofinance.com}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = String.format("%s/reset-password?token=%s", frontendUrl, token);

        if (mailSender == null) {
            log.info("JavaMailSender não configurado. Link de recuperação para {}: {}", toEmail, resetLink);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Recuperação de Senha - Nexo Finance");

            String htmlContent = buildResetPasswordEmailHtml(resetLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("E-mail de recuperação de senha enviado com sucesso para: {}", toEmail);

        } catch (Exception ex) {
            log.warn("Não foi possível enviar o e-mail via SMTP ({}). Link de redefinição gerado para teste: {}",
                    ex.getMessage(), resetLink);
        }
    }

    private String buildResetPasswordEmailHtml(String resetLink) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px;">
                <h2 style="color: #2563eb; text-align: center;">Nexo Finance</h2>
                <p>Olá,</p>
                <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                <p>Clique no botão abaixo para criar uma nova senha:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">
                        Redefinir Minha Senha
                    </a>
                </div>
                <p style="color: #64748b; font-size: 0.9em;">Este link é válido por <strong>30 minutos</strong>.</p>
                <p style="color: #64748b; font-size: 0.9em;">Se você não solicitou a alteração, ignore este e-mail. Sua senha permanecerá a mesma.</p>
            </div>
            """.formatted(resetLink);
    }
}
