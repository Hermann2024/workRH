package com.workrh.leave.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.leave.api.dto.LeaveDecisionRequestDto;
import com.workrh.leave.api.dto.LeaveRequestDto;
import com.workrh.leave.api.dto.LeaveResponseDto;
import com.workrh.leave.service.LeaveService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
@RequiresFeature(FeatureCode.LEAVE_MANAGEMENT)
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    public LeaveResponseDto create(@Valid @RequestBody LeaveRequestDto request) {
        return leaveService.create(request);
    }

    @GetMapping("/{leaveId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    public LeaveResponseDto findById(@PathVariable Long leaveId) {
        return leaveService.findById(leaveId);
    }

    @PostMapping("/{leaveId}/approve")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public LeaveResponseDto approve(@PathVariable Long leaveId, @Valid @RequestBody LeaveDecisionRequestDto request) {
        return leaveService.approve(leaveId, request);
    }

    @PostMapping("/{leaveId}/reject")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public LeaveResponseDto reject(@PathVariable Long leaveId, @Valid @RequestBody LeaveDecisionRequestDto request) {
        return leaveService.reject(leaveId, request);
    }

    @PostMapping("/{leaveId}/cancel")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    public LeaveResponseDto cancel(@PathVariable Long leaveId, @Valid @RequestBody LeaveDecisionRequestDto request) {
        return leaveService.cancel(leaveId, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public List<LeaveResponseDto> list(@RequestParam(required = false) Long employeeId) {
        return employeeId == null ? leaveService.list() : leaveService.listByEmployee(employeeId);
    }
}
