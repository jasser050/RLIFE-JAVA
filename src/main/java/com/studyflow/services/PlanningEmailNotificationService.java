package com.studyflow.services;

import com.studyflow.models.Planning;
import com.studyflow.models.Seance;
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
    private enum NotificationType {
        MATCH,
        DAY_OFF,
        STUDY
    }

    private record EmailTheme(
            String icon,
            String gradient,
            String highlight,
            String panelBackground,
            String panelBorder,
            String title,
            String subtitle,
            String intro,
            String closing,
            String badge
    ) {
    }

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
        return sendPlanningCreatedEmail(user, planning, null);
    }

    public String sendPlanningCreatedEmail(User user, Planning planning, Seance seance) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return "User email missing";
        }

        if (planning == null) {
            return "Planning missing";
        }

        try {
            NotificationType type = resolveNotificationType(planning, seance);
            String htmlBody = buildHtmlMessage(user, planning, type);
            String subject = buildSubject(type);
            sendEmail(user.getEmail(), subject, htmlBody);
            return null;

        } catch (Exception e) {
            return "Email error: " + e.getMessage();
        }
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) throws Exception {

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
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    private String buildHtmlMessage(User user, Planning p, NotificationType type) {

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

        EmailTheme theme = buildTheme(type);

        return "<!DOCTYPE html>"
                + "<html><body style='margin:0;padding:0;background:#f4f7fb;font-family:Segoe UI,Arial,sans-serif;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='padding:28px 12px;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' width='620' cellspacing='0' cellpadding='0' style='max-width:620px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(2,6,23,0.12);'>"
                + "<tr><td style='background:" + escapeHtml(theme.gradient()) + ";padding:26px 24px;text-align:center;color:#ffffff;'>"
                + logoHtml
                + "<div style='font-size:34px;line-height:1;margin:6px 0 8px 0;'>" + escapeHtml(theme.icon()) + "</div>"
                + "<div style='font-size:22px;font-weight:700;'>" + escapeHtml(theme.title()) + "</div>"
                + "<div style='font-size:14px;opacity:.95;margin-top:6px;'>" + escapeHtml(theme.subtitle()) + "</div>"
                + "</td></tr>"
                + "<tr><td style='padding:24px;color:#0f172a;'>"
                + "<p style='margin:0 0 16px 0;font-size:15px;'>Hello <strong>" + safeName + "</strong>,</p>"
                + "<p style='margin:0 0 18px 0;font-size:14px;color:#334155;'>" + escapeHtml(theme.intro()) + "</p>"
                + "<div style='margin:0 0 14px 0;'>"
                + "<span style='display:inline-block;padding:7px 12px;border-radius:999px;background:" + escapeHtml(theme.highlight()) + ";color:#ffffff;font-size:12px;font-weight:700;letter-spacing:.25px;'>"
                + escapeHtml(theme.badge())
                + "</span>"
                + "</div>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background:" + escapeHtml(theme.panelBackground()) + ";border:1px solid " + escapeHtml(theme.panelBorder()) + ";border-radius:12px;'>"
                + "<tr><td style='padding:16px 18px;font-size:14px;color:#0f172a;'>"
                + "<div style='margin-bottom:8px;'><strong>Title:</strong> " + safeTitle + "</div>"
                + "<div style='margin-bottom:8px;'><strong>Date:</strong> " + safeDate + "</div>"
                + "<div><strong>Time:</strong> " + safeStart + " - " + safeEnd + "</div>"
                + "</td></tr></table>"
                + "<p style='margin:20px 0 0 0;font-size:14px;color:#334155;'>" + escapeHtml(theme.closing()) + "</p>"
                + "</td></tr>"
                + "<tr><td style='padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:12px;color:#64748b;'>Rlife Team</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private NotificationType resolveNotificationType(Planning planning, Seance seance) {
        String seanceTypeName = normalize(seance == null ? null : seance.getTypeSeanceName());
        String seanceType = normalize(seance == null ? null : seance.getTypeSeance());
        String title = normalize(planning == null ? null : planning.getSeanceTitle());

        if (seanceTypeName.contains("day off") || seanceType.contains("day off")
                || seanceTypeName.contains("dayoff") || seanceType.contains("dayoff")
                || title.contains("day off") || title.contains("dayoff")) {
            return NotificationType.DAY_OFF;
        }

        if (seanceTypeName.contains("match") || seanceType.contains("match") || title.contains(" vs ")) {
            return NotificationType.MATCH;
        }

        return NotificationType.STUDY;
    }

    private String buildSubject(NotificationType type) {
        return switch (type) {
            case MATCH -> "Rlife - Match planned";
            case DAY_OFF -> "Rlife - Day off planned";
            default -> "Rlife - Study session planned";
        };
    }

    private EmailTheme buildTheme(NotificationType type) {
        return switch (type) {
            case MATCH -> new EmailTheme(
                    "⚽",
                    "linear-gradient(135deg,#0f766e,#0ea5e9)",
                    "#0f766e",
                    "#f0fdfa",
                    "#99f6e4",
                    "Match Planned Successfully",
                    "Your next football moment is ready with Rlife",
                    "Great news! Your match session has been added to your planning.",
                    "Enjoy the game and have an amazing match day.",
                    "MATCH PLANNER"
            );
            case DAY_OFF -> new EmailTheme(
                    "🌴",
                    "linear-gradient(135deg,#16a34a,#84cc16)",
                    "#15803d",
                    "#f7fee7",
                    "#bef264",
                    "Day Off Confirmed",
                    "Recharge mode enabled with Rlife",
                    "Your day off has been planned successfully. Time to breathe and reset.",
                    "Enjoy your break and come back stronger.",
                    "DAY OFF"
            );
            default -> new EmailTheme(
                    "📚",
                    "linear-gradient(135deg,#1d4ed8,#7c3aed)",
                    "#4338ca",
                    "#f8fafc",
                    "#cbd5e1",
                    "Study Session Confirmed",
                    "Stay focused and keep progressing with Rlife",
                    "Your study session has been scheduled successfully. Here are your details:",
                    "Good luck with your study session.",
                    "STUDY SESSION"
            );
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ENGLISH);
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