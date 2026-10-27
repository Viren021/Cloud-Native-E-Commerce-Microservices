package com.ecommerce.notificationservice.service.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import com.ecommerce.commonlib.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {
    private JavaMailSender sender;
    private EmailServiceImpl service;

    @BeforeEach void setUp() {
        sender = mock(JavaMailSender.class);
        service = new EmailServiceImpl(sender);
        ReflectionTestUtils.setField(service, "fromEmail", "sender@example.test");
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test void rejectsPathLikeOrUnsupportedAttachmentsBeforeSending() {
        var file = new MockMultipartFile("file", "../../secrets.txt", "text/plain", "secret".getBytes());
        assertThrows(BusinessException.class, () -> service.sendMail(new org.springframework.web.multipart.MultipartFile[]{file}, "to@example.test", new String[0], "subject", "body"));
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test void rejectsOversizedAttachmentBeforeLoadingItIntoMail() {
        var bytes = new byte[5 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("file", "large.pdf", "application/pdf", bytes);
        assertThrows(BusinessException.class, () -> service.sendMail(new org.springframework.web.multipart.MultipartFile[]{file}, "to@example.test", new String[0], "subject", "body"));
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test void allowsSupportedBoundedPdfAttachmentForAuthorizedControllerWorkflow() {
        var file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "%PDF-1.7 test".getBytes());
        service.sendMail(new org.springframework.web.multipart.MultipartFile[]{file}, "to@example.test", new String[0], "subject", "body");
        verify(sender).send(any(MimeMessage.class));
    }
}
