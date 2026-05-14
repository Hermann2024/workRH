package com.workrh.users.api;

import com.workrh.users.api.dto.PlatformWorkspaceResponse;
import com.workrh.users.service.EmployeeService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/workspaces")
public class PlatformWorkspaceController {

    private final EmployeeService employeeService;

    public PlatformWorkspaceController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<PlatformWorkspaceResponse> listWorkspaces() {
        return employeeService.platformWorkspaces();
    }
}
