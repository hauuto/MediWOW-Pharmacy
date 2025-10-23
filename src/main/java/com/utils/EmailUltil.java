package com.utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;


/**
 * Author: Tô Thanh Hậu
 */

public class EmailUltil {


    private final Session session;
    private final String from;

    public EmailUltil(String host, int port, String username, String password, String from) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));

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
            message.setSubject("Thông tin tài khoản của bạn");
            String name = (fullName != null && !fullName.trim().isEmpty()) ? fullName : "Người dùng";
            String body = String.format(
                    "Xin chào %s,\n\n" +
                    "Đây là thông tin tài khoản của bạn:\n" +
                    "Tên đăng nhập: %s\n" +
                    "Mật khẩu: %s\n\n" +
                    "Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu.\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ MediWOW",
                    name, username, plainPassword
            );
            message.setText(body);
            Transport.send(message);
        }catch (MessagingException e){
            throw new RuntimeException(e);
        }
    }
}
