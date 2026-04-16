package com.workrh.notification.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.notification.api.dto.NotificationResponseDto;
import com.workrh.notification.api.dto.SmsNotificationRequest;
import com.workrh.notification.api.dto.SmsNotificationResponse;
import com.workrh.notification.service.NotificationService;
import com.workrh.notification.service.SmsNotificationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SmsNotificationService smsNotificationService;

    public NotificationController(NotificationService notificationService, SmsNotificationService smsNotificationService) {
        this.notificationService = notificationService;
        this.smsNotificationService = smsNotificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EMAIL_NOTIFICATIONS)
    public List<NotificationResponseDto> list() {
        return notificationService.list();
    }

    @PostMapping("/sms")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.SMS_NOTIFICATIONS)
    public SmsNotificationResponse sendSms(@RequestBody SmsNotificationRequest request) {
        return smsNotificationService.send(request);
    }
}
