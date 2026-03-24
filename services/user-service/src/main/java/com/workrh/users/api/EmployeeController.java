package com.workrh.users.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.users.api.dto.EmployeeCreateRequest;
import com.workrh.users.api.dto.EmployeeResponse;
import com.workrh.users.api.dto.EmployeeUpdateRequest;
import com.workrh.users.api.dto.PasswordUpdateRequest;
import com.workrh.users.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiresFeature(FeatureCode.EMPLOYEE_MANAGEMENT)
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public List<EmployeeResponse> findAll() {
        return employeeService.findAll();
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public EmployeeResponse findById(@PathVariable Long employeeId) {
        return employeeService.findById(employeeId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    public EmployeeResponse currentProfile() {
        return employeeService.currentProfile();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public EmployeeResponse update(@PathVariable Long employeeId, @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.update(employeeId, request);
    }

    @PatchMapping("/{employeeId}/password")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public EmployeeResponse updatePassword(@PathVariable Long employeeId, @Valid @RequestBody PasswordUpdateRequest request) {
        return employeeService.updatePassword(employeeId, request);
    }

    @PatchMapping("/{employeeId}/activate")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public EmployeeResponse activate(@PathVariable Long employeeId) {
        return employeeService.setActive(employeeId, true);
    }

    @PatchMapping("/{employeeId}/deactivate")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public EmployeeResponse deactivate(@PathVariable Long employeeId) {
        return employeeService.setActive(employeeId, false);
    }
}
