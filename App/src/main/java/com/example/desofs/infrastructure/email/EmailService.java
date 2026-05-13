package com.example.desofs.infrastructure.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Email Service for sending order confirmations, refund notifications, MFA codes, etc.
 * To be implemented with Spring Mail and template engine
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@example.com}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send order confirmation email
     * @param email recipient email
     * @param orderId order ID
     */
    public void sendOrderConfirmation(String email, Long orderId) {
        sendEmail(email, "Order Confirmation #" + orderId, loadTemplate("templates/email/order-confirmation.html") + "\nOrder ID: " + orderId);
    }

    /**
     * Send refund notification email
     * @param email recipient email
     * @param refundId refund request ID
     * @param status refund status (APPROVED, REJECTED, COMPLETED)
     */
    public void sendRefundNotification(String email, Long refundId, String status) {
        sendEmail(email, "Refund Update #" + refundId, loadTemplate("templates/email/refund-notification.html") + "\nStatus: " + status);
    }

    /**
     * Send email OTP code for MFA
     * @param email recipient email
     * @param otpCode OTP code to send
     */
    public void sendEmailOtp(String email, String otpCode) {
        sendEmail(email, "Your MFA Code", loadTemplate("templates/email/mfa-otp.html") + "\nCode: " + otpCode);
    }

    /**
     * Send generic email
     * @param email recipient email
     * @param subject email subject
     * @param body email body
     */
    public void sendEmail(String email, String subject, String body) {
        if (mailSender == null) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String loadTemplate(String templatePath) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            if (!resource.exists()) {
                return "";
            }
            return Files.readString(Paths.get(resource.getURI()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }
}
