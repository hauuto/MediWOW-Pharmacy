package com.utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Properties;


/**
 * Author: Tô Thanh Hậu
 */

public class EmailUltil {

    private static final String PASSWORD_TEMPLATE_PATH = "templates/password_email.html";

    private final Session session;
    private final String from;

    public EmailUltil(String host, int port, String username, String password, String from) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        // Optionally enforce modern TLS in some environments
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        this.from = from;
        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }


    public void sendPasswordEmail(String to, String fullName, String username, String plainPassword){
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Thông tin đăng nhập hệ thống MediWOW");

            String name = (fullName != null && !fullName.trim().isEmpty()) ? fullName : "Người dùng";

            // Plain-text fallback
            String textBody = String.format(
                    "Xin chào %s,\n\n" +
                    "Đây là thông tin tài khoản của bạn:\n" +
                    "Tên đăng nhập: %s\n" +
                    "Mật khẩu: %s\n\n" +
                    "Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu.\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ MediWOW",
                    name, username, plainPassword
            );

            // Themed HTML
            String htmlBody = renderPasswordEmailHtml(name, username, plainPassword);

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(textBody, StandardCharsets.UTF_8.name());

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

            MimeMultipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);

            Transport.send(message);
        }catch (MessagingException e){
            throw new RuntimeException(e);
        }
    }

    private String renderPasswordEmailHtml(String name, String username, String password) {
        String template = loadResourceAsString(PASSWORD_TEMPLATE_PATH);
        String year = String.valueOf(Year.now().getValue());
        return template
                .replace("{{name}}", safe(name))
                .replace("{{username}}", safe(username))
                .replace("{{password}}", safe(password))
                .replace("{{year}}", year);
    }

    private String loadResourceAsString(String resourcePath) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = EmailUltil.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Email template not found on classpath: " + resourcePath);
            }
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read email template: " + resourcePath, ex);
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        // Minimal HTML escaping for common characters used in names/usernames
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
