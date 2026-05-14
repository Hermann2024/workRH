package com.workrh.users.api;

import com.workrh.common.security.InternalRequestGuard;
import com.workrh.users.api.dto.TenantWorkspaceResponse;
import com.workrh.users.api.dto.WorkspaceSubscriptionSyncRequest;
import com.workrh.users.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/workspaces")
public class InternalWorkspaceController {

    private final EmployeeService employeeService;

    @Value("${workspace.internal.key:}")
    private String internalKey;

    public InternalWorkspaceController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/subscription")
    public TenantWorkspaceResponse syncSubscription(
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey,
            @Valid @RequestBody WorkspaceSubscriptionSyncRequest request) {
        InternalRequestGuard.requireValidKey(internalKey, providedKey, "workspace internal key");
        return employeeService.syncWorkspaceSubscription(request);
    }
}
