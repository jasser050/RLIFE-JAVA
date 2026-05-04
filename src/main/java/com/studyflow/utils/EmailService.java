package com.studyflow.utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    // Gmail SMTP configuration — use an App Password (not your regular password)
    // To generate: Google Account → Security → 2-Step Verification → App passwords
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SENDER_EMAIL = "jasserbalti555@gmail.com";
    private static final String SENDER_PASSWORD = "dorh aoic sira qxkp";

    public static void sendPasswordResetEmail(String toEmail, String resetCode) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL, false));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("RLife — Password Reset Code");

        String htmlBody = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto; background: #0F172A; border-radius: 16px; padding: 40px; color: #E2E8F0;">
                    <div style="text-align: center; margin-bottom: 32px;">
                        <h1 style="color: #A78BFA; margin: 0; font-size: 28px;">RLife</h1>
                        <p style="color: #64748B; margin-top: 4px;">Password Reset</p>
                    </div>
                    <p style="font-size: 15px; line-height: 1.6;">You requested a password reset. Use the code below to reset your password:</p>
                    <div style="background: linear-gradient(135deg, #6D28D9, #8B5CF6); border-radius: 12px; padding: 24px; text-align: center; margin: 24px 0;">
                        <span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: white;">%s</span>
                    </div>
                    <p style="font-size: 13px; color: #64748B; text-align: center;">This code expires in 10 minutes.<br>If you didn't request this, ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #1E293B; margin: 24px 0;">
                    <p style="font-size: 11px; color: #475569; text-align: center;">© 2026 RLife — Student Productivity Platform</p>
                </div>
                """.formatted(resetCode);

        message.setContent(htmlBody, "text/html; charset=utf-8");
        Transport.send(message);
    }

    /**
     * Generate a 6-digit numeric reset code.
     */
    public static String generateResetCode() {
        int code = 100000 + new java.util.Random().nextInt(900000);
        return String.valueOf(code);
    }
}
