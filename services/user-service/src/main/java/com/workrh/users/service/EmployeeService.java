package com.workrh.users.service;

import com.workrh.common.security.JwtService;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.common.web.UnauthorizedException;
import com.workrh.users.api.dto.EmployeeCreateRequest;
import com.workrh.users.api.dto.EmployeeResponse;
import com.workrh.users.api.dto.LoginRequest;
import com.workrh.users.api.dto.LoginResponse;
import com.workrh.users.api.dto.EmployeeUpdateRequest;
import com.workrh.users.api.dto.PasswordUpdateRequest;
import com.workrh.users.domain.Employee;
import com.workrh.users.repository.EmployeeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public EmployeeResponse create(EmployeeCreateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (employeeRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
            throw new BadRequestException("Employee email already exists for tenant");
        }
        Employee employee = new Employee();
        employee.setTenantId(tenantId);
        employee.setEmail(request.email());
        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setCountryOfResidence(request.countryOfResidence());
        employee.setPhoneNumber(request.phoneNumber());
        employee.setDepartment(request.department());
        employee.setJobTitle(request.jobTitle());
        employee.setCrossBorderWorker(request.crossBorderWorker());
        employee.setHireDate(request.hireDate());
        employee.setRoles(request.roles());
        employee.setCreatedAt(Instant.now());
        employee.setUpdatedAt(Instant.now());
        return toResponse(employeeRepository.save(employee));
    }

    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAllByTenantId(TenantContext.getTenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public EmployeeResponse findById(Long employeeId) {
        return toResponse(getEmployee(employeeId));
    }

    public EmployeeResponse update(Long employeeId, EmployeeUpdateRequest request) {
        String tenantId = TenantContext.getTenantId();
        Employee employee = getEmployee(employeeId);
        if (employeeRepository.existsByEmailAndTenantIdAndIdNot(request.email(), tenantId, employeeId)) {
            throw new BadRequestException("Employee email already exists for tenant");
        }
        employee.setEmail(request.email());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setCountryOfResidence(request.countryOfResidence());
        employee.setPhoneNumber(request.phoneNumber());
        employee.setDepartment(request.department());
        employee.setJobTitle(request.jobTitle());
        employee.setCrossBorderWorker(request.crossBorderWorker());
        employee.setHireDate(request.hireDate());
        employee.setRoles(request.roles());
        if (request.active() != null) {
            employee.setActive(request.active());
        }
        employee.setUpdatedAt(Instant.now());
        return toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse updatePassword(Long employeeId, PasswordUpdateRequest request) {
        Employee employee = getEmployee(employeeId);
        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setUpdatedAt(Instant.now());
        return toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse setActive(Long employeeId, boolean active) {
        Employee employee = getEmployee(employeeId);
        employee.setActive(active);
        employee.setUpdatedAt(Instant.now());
        return toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse currentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Missing authenticated user");
        }
        Employee employee = employeeRepository.findByEmailAndTenantId(authentication.getName(), TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Authenticated employee not found"));
        return toResponse(employee);
    }

    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByEmailAndTenantId(request.email(), TenantContext.getTenantId())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!employee.isActive()) {
            throw new UnauthorizedException("Employee account is disabled");
        }

        Set<String> roles = employee.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        String token = jwtService.generateToken(employee.getEmail(), employee.getTenantId(), roles.stream().toList());
        return new LoginResponse(token, employee.getTenantId(), roles);
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findByIdAndTenantId(employeeId, TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmail(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getCountryOfResidence(),
                employee.getPhoneNumber(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.isCrossBorderWorker(),
                employee.getHireDate(),
                employee.isActive(),
                employee.getRoles(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
