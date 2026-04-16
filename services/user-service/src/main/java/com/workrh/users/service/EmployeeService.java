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
import com.workrh.users.api.dto.SignupRequest;
import com.workrh.users.domain.Employee;
import com.workrh.users.domain.Role;
import com.workrh.users.repository.EmployeeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SubscriptionBootstrapClient subscriptionBootstrapClient;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SubscriptionBootstrapClient subscriptionBootstrapClient) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.subscriptionBootstrapClient = subscriptionBootstrapClient;
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
        String tenantId = normalizeTenantId(TenantContext.getTenantId());
        String email = normalizeEmail(request.email());
        Employee employee = employeeRepository.findByEmailAndTenantId(email, tenantId)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!employee.isActive()) {
            throw new UnauthorizedException("Employee account is disabled");
        }

        Set<String> roles = employee.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        String token = jwtService.generateToken(employee.getEmail(), employee.getTenantId(), roles.stream().toList(), employee.getId());
        return new LoginResponse(token, employee.getTenantId(), roles);
    }

    @Transactional
    public LoginResponse signup(SignupRequest request) {
        String tenantId = normalizeTenantId(TenantContext.getTenantId());
        String email = normalizeEmail(request.email());
        ensureSignupAvailable(tenantId, email);
        String trialPlanCode = resolveTrialPlanCode(request.planCode());

        Employee employee = new Employee();
        employee.setTenantId(tenantId);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setCountryOfResidence("LU");
        employee.setDepartment("Administration");
        employee.setJobTitle("Administrateur RH");
        employee.setCrossBorderWorker(false);
        employee.setHireDate(LocalDate.now());
        employee.setActive(true);
        employee.setRoles(Set.of(Role.ADMIN, Role.HR));
        employee.setCreatedAt(Instant.now());
        employee.setUpdatedAt(Instant.now());

        Employee saved = employeeRepository.save(employee);
        subscriptionBootstrapClient.initializeTrial(tenantId, saved.getEmail(), request.seatsPurchased(), trialPlanCode);

        Set<String> roles = saved.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        String token = jwtService.generateToken(saved.getEmail(), tenantId, roles.stream().toList(), saved.getId());
        return new LoginResponse(token, tenantId, roles);
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findByIdAndTenantId(employeeId, TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private void ensureSignupAvailable(String tenantId, String email) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Tenant identifier is required");
        }
        if (tenantId.length() < 3) {
            throw new BadRequestException("Tenant identifier must contain at least 3 characters");
        }
        if (employeeRepository.existsByTenantId(tenantId)) {
            throw new BadRequestException("This workspace already exists");
        }
        if (employeeRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new BadRequestException("This email is already used in the selected workspace");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeTenantId(String tenantId) {
        if (tenantId == null) {
            return "";
        }
        String normalized = tenantId.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "")
                .replaceAll("-{2,}", "-");
        return normalized;
    }

    private String resolveTrialPlanCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return "STARTER";
        }

        String normalized = planCode.trim().toUpperCase();
        return switch (normalized) {
            case "STARTER", "PRO", "PREMIUM" -> normalized;
            default -> "STARTER";
        };
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
