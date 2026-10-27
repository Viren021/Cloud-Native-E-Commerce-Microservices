package com.ecommerce.notificationservice.service.impl;

import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.commonlib.exception.ErrorCode;
import com.ecommerce.notificationservice.dto.EmailDetails;
import com.ecommerce.notificationservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public String sendSimpleMail(EmailDetails details) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(details.getRecipient());
            mailMessage.setText(details.getMsgBody());
            mailMessage.setSubject(details.getSubject());
            javaMailSender.send(mailMessage);
            return "Mail Sent Successfully";
        } catch (Exception ex) {
            log.error("Error while sending mail", ex);
            return "Error while Sending Mail";
        }
    }

    @Override
    public String sendMail(MultipartFile[] files, String to, String[] cc, String subject, String body) {
        validateAttachments(files);
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setCc(cc);
            helper.setSubject(subject);
            helper.setText(body);
            if (files != null) for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                helper.addAttachment(safeFilename(file.getOriginalFilename()),
                        new ByteArrayResource(file.getBytes()));
            }
            javaMailSender.send(mimeMessage);
            return "Email sent successfully";
        } catch (Exception ex) {
            log.error("Error while sending email", ex);
            return "Error while Sending Email";
        }
    }

    private void validateAttachments(MultipartFile[] files) {
        if (files == null) return;
        long total = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            if (file.getSize() > 5L * 1024 * 1024) throw BusinessException.of(ErrorCode.BAD_REQUEST);
            total += file.getSize();
            if (total > 10L * 1024 * 1024) throw BusinessException.of(ErrorCode.BAD_REQUEST);
            String name = safeFilename(file.getOriginalFilename());
            String type = file.getContentType();
            byte[] prefix;
            try (var input = file.getInputStream()) { prefix = input.readNBytes(8); }
            catch (IOException ex) { throw BusinessException.badRequest("error.bad.request", ex); }
            boolean valid = (name.toLowerCase().endsWith(".pdf") && "application/pdf".equalsIgnoreCase(type)
                    && prefix.length >= 5 && prefix[0] == '%' && prefix[1] == 'P' && prefix[2] == 'D' && prefix[3] == 'F' && prefix[4] == '-')
                    || (name.toLowerCase().endsWith(".png") && "image/png".equalsIgnoreCase(type)
                    && prefix.length >= 8 && (prefix[0] & 0xff) == 0x89 && prefix[1] == 'P' && prefix[2] == 'N' && prefix[3] == 'G')
                    || ((name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"))
                    && "image/jpeg".equalsIgnoreCase(type) && prefix.length >= 3
                    && (prefix[0] & 0xff) == 0xff && (prefix[1] & 0xff) == 0xd8 && (prefix[2] & 0xff) == 0xff);
            if (!valid) throw BusinessException.of(ErrorCode.BAD_REQUEST);
        }
    }

    private String safeFilename(String original) {
        if (original == null || original.isBlank() || original.contains("/") || original.contains("\\")
                || original.contains("..") || original.length() > 128) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST);
        }
        return original;
    }
}
