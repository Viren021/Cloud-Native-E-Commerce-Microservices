package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.EmailDetails;
import com.ecommerce.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/sendSimpleMail")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String sendSimpleMail(@RequestBody EmailDetails details) {
        return emailService.sendSimpleMail(details);
    }

    @PostMapping("/sendMail")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String sendMail(@RequestParam(value = "file", required = false) MultipartFile[] files,
                           @RequestParam String to,
                           @RequestParam String[] cc,
                           @RequestParam String subject,
                           @RequestParam String body) {
        return emailService.sendMail(files, to, cc, subject, body);
    }
}
