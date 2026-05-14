package com.workrh.users.api;

import com.workrh.users.api.dto.EmployeeInvitationAcceptRequest;
import com.workrh.users.api.dto.EmployeeInvitationPreviewResponse;
import com.workrh.users.api.dto.LoginRequest;
import com.workrh.users.api.dto.LoginResponse;
import com.workrh.users.api.dto.PasswordResetConfirmRequest;
import com.workrh.users.api.dto.PasswordResetRequest;
import com.workrh.users.api.dto.PasswordResetRequestResponse;
import com.workrh.users.api.dto.SignupRequest;
import com.workrh.users.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeService employeeService;

    public AuthController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return employeeService.login(request);
    }

    @PostMapping("/signup")
    public LoginResponse signup(@Valid @RequestBody SignupRequest request) {
        return employeeService.signup(request);
    }

    @GetMapping("/invitations")
    public EmployeeInvitationPreviewResponse previewInvitation(@RequestParam("token") String token) {
        return employeeService.previewInvitation(token);
    }

    @PostMapping("/invitations/accept")
    public LoginResponse acceptInvitation(@Valid @RequestBody EmployeeInvitationAcceptRequest request) {
        return employeeService.acceptInvitation(request);
    }

    @PostMapping("/password-reset/request")
    public PasswordResetRequestResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return employeeService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    public LoginResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return employeeService.confirmPasswordReset(request);
    }
}
