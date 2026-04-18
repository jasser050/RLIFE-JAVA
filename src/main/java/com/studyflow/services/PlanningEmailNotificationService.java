package com.studyflow.services;

import com.studyflow.models.Planning;
import com.studyflow.models.User;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;

public class PlanningEmailNotificationService {

    // 🔴 TON COMPTE GMAIL
    private static final String SMTP_USER = "mmymmy512@gmail.com";
    private static final String SMTP_PASSWORD = "qnln prqr owxo btit";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SENDER_NAME = "Rlife";
    private static final String LOGO_RESOURCE = "/com/studyflow/assets/rlife-logo.svg";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public String sendPlanningCreatedEmail(User user, Planning planning) {

        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return "User email missing";
        }

        if (planning == null) {
            return "Planning missing";
        }

        try {
            String htmlBody = buildHtmlMessage(user, planning);
            sendEmail(user.getEmail(), htmlBody);
            return null;

        } catch (Exception e) {
            return "Email error: " + e.getMessage();
        }
    }

    private void sendEmail(String toEmail, String htmlBody) throws Exception {

        Properties props = new Properties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });

        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(SMTP_USER, SENDER_NAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Rlife - Session Planned", StandardCharsets.UTF_8.name());
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    private String buildHtmlMessage(User user, Planning p) {

        String name = (user.getFirstName() == null) ? "Student" : user.getFirstName();

        String date = (p.getPlanningDate() == null)
                ? "N/A"
                : p.getPlanningDate().format(DATE_FORMATTER);

        String start = (p.getStartTime() == null)
                ? "N/A"
                : p.getStartTime().format(TIME_FORMATTER);

        String end = (p.getEndTime() == null)
                ? "N/A"
                : p.getEndTime().format(TIME_FORMATTER);

        String title = (p.getSeanceTitle() == null) ? "Session" : p.getSeanceTitle();

        String logoDataUri = loadLogoDataUri();
        String logoHtml = logoDataUri.isBlank()
                ? "<div style='font-size:28px;font-weight:800;color:#0f172a;'>Rlife</div>"
                : "<img src='" + logoDataUri + "' alt='Rlife logo' style='width:120px;height:auto;display:block;margin:0 auto 12px auto;'/>";

        String safeName = escapeHtml(name);
        String safeTitle = escapeHtml(title);
        String safeDate = escapeHtml(date);
        String safeStart = escapeHtml(start);
        String safeEnd = escapeHtml(end);

        return "<!DOCTYPE html>"
                + "<html><body style='margin:0;padding:0;background:#f4f7fb;font-family:Segoe UI,Arial,sans-serif;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='padding:28px 12px;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' width='620' cellspacing='0' cellpadding='0' style='max-width:620px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(2,6,23,0.12);'>"
                + "<tr><td style='background:linear-gradient(135deg,#1d4ed8,#7c3aed);padding:26px 24px;text-align:center;color:#ffffff;'>"
                + logoHtml
                + "<div style='font-size:22px;font-weight:700;'>Your session is confirmed</div>"
                + "<div style='font-size:14px;opacity:.95;margin-top:6px;'>Plan smarter with Rlife</div>"
                + "</td></tr>"
                + "<tr><td style='padding:24px;color:#0f172a;'>"
                + "<p style='margin:0 0 16px 0;font-size:15px;'>Hello <strong>" + safeName + "</strong>,</p>"
                + "<p style='margin:0 0 18px 0;font-size:14px;color:#334155;'>Your planning was created successfully. Here are your session details:</p>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;'>"
                + "<tr><td style='padding:16px 18px;font-size:14px;color:#0f172a;'>"
                + "<div style='margin-bottom:8px;'><strong>Title:</strong> " + safeTitle + "</div>"
                + "<div style='margin-bottom:8px;'><strong>Date:</strong> " + safeDate + "</div>"
                + "<div><strong>Time:</strong> " + safeStart + " - " + safeEnd + "</div>"
                + "</td></tr></table>"
                + "<p style='margin:20px 0 0 0;font-size:14px;color:#334155;'>Good luck with your study session.</p>"
                + "</td></tr>"
                + "<tr><td style='padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:12px;color:#64748b;'>Rlife Team</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String loadLogoDataUri() {
        try (var stream = PlanningEmailNotificationService.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (stream == null) {
                return "";
            }
            byte[] bytes = stream.readAllBytes();
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}