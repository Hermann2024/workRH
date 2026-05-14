package com.workrh.notification.api;

import com.workrh.common.security.InternalRequestGuard;
import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.notification.api.dto.EmployeeInvitationEmailRequest;
import com.workrh.notification.api.dto.NotificationResponseDto;
import com.workrh.notification.api.dto.PasswordResetEmailRequest;
import com.workrh.notification.api.dto.SmsNotificationRequest;
import com.workrh.notification.api.dto.SmsNotificationResponse;
import com.workrh.notification.service.NotificationService;
import com.workrh.notification.service.SmsNotificationService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @Value("${notification.internal.key:}")
    private String internalKey;

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

    @PostMapping("/invitations")
    public InvitationEmailResponse sendInvitation(
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey,
            @RequestBody EmployeeInvitationEmailRequest request) {
        InternalRequestGuard.requireValidKey(internalKey, providedKey, "notification internal key");
        return new InvitationEmailResponse(notificationService.sendEmployeeInvitation(request));
    }

    @PostMapping("/password-reset")
    public InvitationEmailResponse sendPasswordReset(
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey,
            @RequestBody PasswordResetEmailRequest request) {
        InternalRequestGuard.requireValidKey(internalKey, providedKey, "notification internal key");
        return new InvitationEmailResponse(notificationService.sendPasswordReset(request));
    }

    public record InvitationEmailResponse(boolean sent) {
    }
}
