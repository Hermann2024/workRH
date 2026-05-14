package com.workrh.users.service;

import com.workrh.common.security.JwtService;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.common.web.UnauthorizedException;
import com.workrh.users.api.dto.EmployeeCreateRequest;
import com.workrh.users.api.dto.EmployeeInvitationAcceptRequest;
import com.workrh.users.api.dto.EmployeeInvitationPreviewResponse;
import com.workrh.users.api.dto.EmployeeInvitationRequest;
import com.workrh.users.api.dto.EmployeeInvitationResponse;
import com.workrh.users.api.dto.EmployeeResponse;
import com.workrh.users.api.dto.LoginRequest;
import com.workrh.users.api.dto.LoginResponse;
import com.workrh.users.api.dto.EmployeeUpdateRequest;
import com.workrh.users.api.dto.PasswordUpdateRequest;
import com.workrh.users.api.dto.PasswordResetConfirmRequest;
import com.workrh.users.api.dto.PasswordResetRequest;
import com.workrh.users.api.dto.PasswordResetRequestResponse;
import com.workrh.users.api.dto.PlatformWorkspaceResponse;
import com.workrh.users.api.dto.SignupRequest;
import com.workrh.users.api.dto.TenantWorkspaceResponse;
import com.workrh.users.api.dto.TenantWorkspaceUpdateRequest;
import com.workrh.users.api.dto.WorkspaceSubscriptionSyncRequest;
import com.workrh.users.domain.Employee;
import com.workrh.users.domain.EmployeeInvitation;
import com.workrh.users.domain.EmployeeGender;
import com.workrh.users.domain.EmploymentContractType;
import com.workrh.users.domain.PasswordResetToken;
import com.workrh.users.domain.Role;
import com.workrh.users.domain.TenantWorkspace;
import com.workrh.users.repository.EmployeeRepository;
import com.workrh.users.repository.EmployeeInvitationRepository;
import com.workrh.users.repository.PasswordResetTokenRepository;
import com.workrh.users.repository.TenantWorkspaceRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
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
    private final TenantWorkspaceRepository tenantWorkspaceRepository;
    private final EmployeeInvitationRepository employeeInvitationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmployeeInvitationNotificationClient invitationNotificationClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SubscriptionBootstrapClient subscriptionBootstrapClient,
            TenantWorkspaceRepository tenantWorkspaceRepository,
            EmployeeInvitationRepository employeeInvitationRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmployeeInvitationNotificationClient invitationNotificationClient) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.subscriptionBootstrapClient = subscriptionBootstrapClient;
        this.tenantWorkspaceRepository = tenantWorkspaceRepository;
        this.employeeInvitationRepository = employeeInvitationRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.invitationNotificationClient = invitationNotificationClient;
    }

    public EmployeeResponse create(EmployeeCreateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (employeeRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
            throw new BadRequestException("Employee email already exists for tenant");
        }
        ensureSeatAvailable(tenantId);
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
        employee.setBirthDate(request.birthDate());
        employee.setGender(resolveGender(request.gender()));
        employee.setContractType(resolveContractType(request.contractType()));
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

    public List<EmployeeInvitationResponse> findInvitations() {
        return employeeInvitationRepository.findAllByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId()).stream()
                .map(invitation -> toInvitationResponse(invitation, null))
                .toList();
    }

    @Transactional
    public EmployeeInvitationResponse inviteEmployee(EmployeeInvitationRequest request) {
        String tenantId = TenantContext.getTenantId();
        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new BadRequestException("This email is already used in the selected workspace");
        }
        if (employeeInvitationRepository.existsByEmailAndTenantIdAndAcceptedAtIsNull(email, tenantId)) {
            throw new BadRequestException("An active invitation already exists for this email");
        }
        ensureSeatAvailable(tenantId);

        String token = generateInvitationToken();
        EmployeeInvitation invitation = new EmployeeInvitation();
        invitation.setTenantId(tenantId);
        invitation.setEmail(email);
        invitation.setFirstName(request.firstName().trim());
        invitation.setLastName(request.lastName().trim());
        invitation.setCountryOfResidence(resolveCountryOfResidence(request.countryOfResidence()));
        invitation.setPhoneNumber(trimToNull(request.phoneNumber()));
        invitation.setDepartment(trimToNull(request.department()));
        invitation.setJobTitle(trimToNull(request.jobTitle()));
        invitation.setBirthDate(request.birthDate());
        invitation.setGender(resolveGender(request.gender()));
        invitation.setContractType(resolveContractType(request.contractType()));
        invitation.setHireDate(request.hireDate() != null ? request.hireDate() : LocalDate.now());
        invitation.setTokenHash(hashInvitationToken(token));
        invitation.setInvitedBy(currentAuthenticatedEmail());
        invitation.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
        invitation.setCreatedAt(Instant.now());
        invitation.setUpdatedAt(Instant.now());
        EmployeeInvitation saved = employeeInvitationRepository.save(invitation);
        TenantWorkspace workspace = tenantWorkspaceRepository.findById(tenantId)
                .orElseGet(() -> fallbackWorkspace(tenantId));
        boolean emailSent = invitationNotificationClient.sendInvitation(saved, workspace, token);
        return toInvitationResponse(saved, token, emailSent);
    }

    public EmployeeInvitationPreviewResponse previewInvitation(String token) {
        EmployeeInvitation invitation = getValidInvitation(token);
        String companyName = tenantWorkspaceRepository.findById(invitation.getTenantId())
                .map(TenantWorkspace::getCompanyName)
                .orElse(invitation.getTenantId());
        return new EmployeeInvitationPreviewResponse(
                invitation.getTenantId(),
                companyName,
                invitation.getEmail(),
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getExpiresAt()
        );
    }

    @Transactional
    public LoginResponse acceptInvitation(EmployeeInvitationAcceptRequest request) {
        EmployeeInvitation invitation = getValidInvitation(request.token());
        if (employeeRepository.existsByEmailAndTenantId(invitation.getEmail(), invitation.getTenantId())) {
            throw new BadRequestException("This employee account already exists");
        }
        ensureSeatAvailable(invitation.getTenantId());

        Employee employee = new Employee();
        employee.setTenantId(invitation.getTenantId());
        employee.setEmail(invitation.getEmail());
        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setFirstName(invitation.getFirstName());
        employee.setLastName(invitation.getLastName());
        employee.setCountryOfResidence(resolveCountryOfResidence(invitation.getCountryOfResidence()));
        employee.setPhoneNumber(invitation.getPhoneNumber());
        employee.setDepartment(invitation.getDepartment());
        employee.setJobTitle(invitation.getJobTitle());
        employee.setBirthDate(invitation.getBirthDate());
        employee.setGender(resolveGender(invitation.getGender()));
        employee.setContractType(resolveContractType(invitation.getContractType()));
        employee.setCrossBorderWorker(!"LU".equalsIgnoreCase(resolveCountryOfResidence(invitation.getCountryOfResidence())));
        employee.setHireDate(invitation.getHireDate() != null ? invitation.getHireDate() : LocalDate.now());
        employee.setActive(true);
        employee.setRoles(Set.of(Role.EMPLOYEE));
        employee.setCreatedAt(Instant.now());
        employee.setUpdatedAt(Instant.now());

        Employee saved = employeeRepository.save(employee);
        invitation.setAcceptedAt(Instant.now());
        invitation.setUpdatedAt(Instant.now());
        employeeInvitationRepository.save(invitation);

        Set<String> roles = saved.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        String jwt = jwtService.generateToken(saved.getEmail(), saved.getTenantId(), roles.stream().toList(), saved.getId());
        return new LoginResponse(jwt, saved.getTenantId(), roles);
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
        employee.setBirthDate(request.birthDate());
        employee.setGender(resolveGender(request.gender()));
        employee.setContractType(resolveContractType(request.contractType()));
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

    public void delete(Long employeeId) {
        Employee employee = getEmployee(employeeId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && employee.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new BadRequestException("You cannot delete your own employee account");
        }
        employeeRepository.delete(employee);
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

    public TenantWorkspaceResponse currentWorkspace() {
        String tenantId = TenantContext.getTenantId();
        TenantWorkspace workspace = tenantWorkspaceRepository.findById(tenantId)
                .orElseGet(() -> fallbackWorkspace(tenantId));
        return toWorkspaceResponse(workspace);
    }

    public List<PlatformWorkspaceResponse> platformWorkspaces() {
        return tenantWorkspaceRepository.findAll().stream()
                .map(this::toPlatformWorkspaceResponse)
                .sorted(java.util.Comparator.comparing(PlatformWorkspaceResponse::createdAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public TenantWorkspaceResponse syncWorkspaceSubscription(WorkspaceSubscriptionSyncRequest request) {
        String tenantId = TenantContext.getTenantId();
        Instant now = Instant.now();
        tenantWorkspaceRepository.upsertSubscriptionFields(
                tenantId,
                tenantId,
                request.ownerEmail() == null || request.ownerEmail().isBlank() ? null : normalizeEmail(request.ownerEmail()),
                request.planCode().trim().toUpperCase(),
                request.seatsPurchased(),
                request.active(),
                now
        );
        TenantWorkspace saved = tenantWorkspaceRepository.findById(tenantId)
                .orElseGet(() -> fallbackWorkspace(tenantId));
        return toWorkspaceResponse(saved);
    }

    @Transactional
    public TenantWorkspaceResponse updateWorkspace(TenantWorkspaceUpdateRequest request) {
        String tenantId = TenantContext.getTenantId();
        TenantWorkspace workspace = tenantWorkspaceRepository.findById(tenantId)
                .orElseGet(() -> fallbackWorkspace(tenantId));
        workspace.setCompanyName(request.companyName().trim());
        workspace.setUpdatedAt(Instant.now());
        TenantWorkspace saved = tenantWorkspaceRepository.save(workspace);
        return toWorkspaceResponse(saved);
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
    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequest request) {
        String tenantId = normalizeTenantId(TenantContext.getTenantId());
        String email = normalizeEmail(request.email());
        if (tenantId.isBlank()) {
            return new PasswordResetRequestResponse(true, false);
        }

        return employeeRepository.findByEmailAndTenantId(email, tenantId)
                .filter(Employee::isActive)
                .map(employee -> {
                    String token = generateInvitationToken();
                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setTenantId(employee.getTenantId());
                    resetToken.setEmail(employee.getEmail());
                    resetToken.setTokenHash(hashInvitationToken(token));
                    resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
                    resetToken.setCreatedAt(Instant.now());
                    resetToken.setUpdatedAt(Instant.now());
                    passwordResetTokenRepository.save(resetToken);

                    TenantWorkspace workspace = tenantWorkspaceRepository.findById(employee.getTenantId())
                            .orElseGet(() -> fallbackWorkspace(employee.getTenantId()));
                    boolean emailSent = invitationNotificationClient.sendPasswordReset(employee, workspace, token);
                    return new PasswordResetRequestResponse(true, emailSent);
                })
                .orElseGet(() -> new PasswordResetRequestResponse(true, false));
    }

    @Transactional
    public LoginResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = getValidPasswordResetToken(request.token());
        Employee employee = employeeRepository.findByEmailAndTenantId(resetToken.getEmail(), resetToken.getTenantId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        if (!employee.isActive()) {
            throw new UnauthorizedException("Employee account is disabled");
        }

        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setUpdatedAt(Instant.now());
        Employee saved = employeeRepository.save(employee);

        resetToken.setUsedAt(Instant.now());
        resetToken.setUpdatedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        Set<String> roles = saved.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        String jwt = jwtService.generateToken(saved.getEmail(), saved.getTenantId(), roles.stream().toList(), saved.getId());
        return new LoginResponse(jwt, saved.getTenantId(), roles);
    }

    @Transactional
    public LoginResponse signup(SignupRequest request) {
        String tenantId = resolveSignupTenantId(request);
        String email = normalizeEmail(request.email());
        ensureTenantIdentifierIsValid(tenantId);
        String accountType = normalizeSignupAccountType(request.accountType());
        if ("EMPLOYEE".equals(accountType)) {
            ensureEmployeeSignupAvailable(tenantId, email);
            return createEmployeeSignupSession(request, tenantId, email);
        }

        ensureHrSignupAvailable(tenantId, email);
        if (request.seatsPurchased() == null) {
            throw new BadRequestException("Seats purchased is required for HR signup");
        }

        Employee employee = new Employee();
        employee.setTenantId(tenantId);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setCountryOfResidence("LU");
        employee.setDepartment("Administration");
        employee.setJobTitle("Administrateur RH");
        employee.setGender(EmployeeGender.AUTRES);
        employee.setContractType(EmploymentContractType.CDI);
        employee.setCrossBorderWorker(false);
        employee.setHireDate(LocalDate.now());
        employee.setActive(true);
        employee.setRoles(Set.of(Role.ADMIN, Role.HR));
        employee.setCreatedAt(Instant.now());
        employee.setUpdatedAt(Instant.now());

        Employee saved = employeeRepository.save(employee);
        createWorkspaceRecord(request, tenantId, saved.getEmail());
        subscriptionBootstrapClient.initializeTrial(
                tenantId,
                saved.getEmail(),
                request.seatsPurchased(),
                resolveTrialPlanCode(request.planCode())
        );

        Set<String> roles = saved.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        String token = jwtService.generateToken(saved.getEmail(), tenantId, roles.stream().toList(), saved.getId());
        return new LoginResponse(token, tenantId, roles);
    }

    private LoginResponse createEmployeeSignupSession(SignupRequest request, String tenantId, String email) {
        Employee employee = new Employee();
        employee.setTenantId(tenantId);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setCountryOfResidence("LU");
        employee.setDepartment("Employes");
        employee.setJobTitle("Salarie");
        employee.setGender(EmployeeGender.AUTRES);
        employee.setContractType(EmploymentContractType.CDI);
        employee.setCrossBorderWorker(false);
        employee.setHireDate(LocalDate.now());
        employee.setActive(true);
        employee.setRoles(Set.of(Role.EMPLOYEE));
        employee.setCreatedAt(Instant.now());
        employee.setUpdatedAt(Instant.now());

        Employee saved = employeeRepository.save(employee);
        Set<String> roles = saved.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        String token = jwtService.generateToken(saved.getEmail(), tenantId, roles.stream().toList(), saved.getId());
        return new LoginResponse(token, tenantId, roles);
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findByIdAndTenantId(employeeId, TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private void ensureTenantIdentifierIsValid(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Tenant identifier is required");
        }
        if (tenantId.length() < 3) {
            throw new BadRequestException("Tenant identifier must contain at least 3 characters");
        }
    }

    private void ensureHrSignupAvailable(String tenantId, String email) {
        if (tenantWorkspaceRepository.existsById(tenantId)) {
            throw new BadRequestException("This workspace already exists");
        }
        if (employeeRepository.existsByTenantId(tenantId)) {
            throw new BadRequestException("This workspace already exists");
        }
        if (employeeRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new BadRequestException("This email is already used in the selected workspace");
        }
    }

    private void ensureEmployeeSignupAvailable(String tenantId, String email) {
        if (!employeeRepository.existsByTenantId(tenantId)) {
            throw new BadRequestException("Employee signup requires an existing workspace");
        }
        if (employeeRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new BadRequestException("This email is already used in the selected workspace");
        }
        ensureSeatAvailable(tenantId);
    }

    private void ensureSeatAvailable(String tenantId) {
        Integer seatsPurchased = tenantWorkspaceRepository.findById(tenantId)
                .map(TenantWorkspace::getSeatsPurchased)
                .orElse(null);
        if (seatsPurchased == null || seatsPurchased <= 0) {
            return;
        }

        long seatsUsed = employeeRepository.countByTenantId(tenantId);
        if (seatsUsed >= seatsPurchased) {
            throw new BadRequestException("Seat limit reached for this workspace. Increase the subscription seats or deactivate unused accounts.");
        }
    }

    private EmployeeInvitation getValidInvitation(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Invitation token is required");
        }
        EmployeeInvitation invitation = employeeInvitationRepository.findByTokenHash(hashInvitationToken(token))
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
        if (invitation.getAcceptedAt() != null) {
            throw new BadRequestException("Invitation already accepted");
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invitation expired");
        }
        return invitation;
    }

    private PasswordResetToken getValidPasswordResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Password reset token is required");
        }
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashInvitationToken(token))
                .orElseThrow(() -> new NotFoundException("Password reset request not found"));
        if (resetToken.getUsedAt() != null) {
            throw new BadRequestException("Password reset link already used");
        }
        if (resetToken.getExpiresAt() != null && resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Password reset link expired");
        }
        return resetToken;
    }

    private String currentAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getName() != null ? authentication.getName() : null;
    }

    private String generateInvitationToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashInvitationToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String resolveCountryOfResidence(String countryOfResidence) {
        String normalized = trimToNull(countryOfResidence);
        return normalized == null ? "FR" : normalized.toUpperCase();
    }

    private EmploymentContractType resolveContractType(EmploymentContractType contractType) {
        return contractType == null ? EmploymentContractType.CDI : contractType;
    }

    private EmployeeGender resolveGender(EmployeeGender gender) {
        return gender == null ? EmployeeGender.AUTRES : gender;
    }

    private TenantWorkspace fallbackWorkspace(String tenantId) {
        TenantWorkspace workspace = new TenantWorkspace();
        workspace.setTenantId(tenantId);
        workspace.setCompanyName(tenantId);
        return workspace;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    private String resolveSignupTenantId(SignupRequest request) {
        String requestedTenantId = normalizeTenantId(request.tenantId());
        if (!requestedTenantId.isBlank()) {
            return requestedTenantId;
        }

        String headerTenantId = normalizeTenantId(TenantContext.getTenantId());
        if (!headerTenantId.isBlank()) {
            return headerTenantId;
        }

        return normalizeTenantId(request.companyName());
    }

    private void createWorkspaceRecord(SignupRequest request, String tenantId, String ownerEmail) {
        tenantWorkspaceRepository.upsertSubscriptionFields(
                tenantId,
                resolveCompanyName(request, tenantId),
                ownerEmail,
                resolveTrialPlanCode(request.planCode()),
                request.seatsPurchased(),
                true,
                Instant.now()
        );
    }

    private String resolveCompanyName(SignupRequest request, String tenantId) {
        String companyName = request.companyName();
        if (companyName != null && !companyName.isBlank()) {
            return companyName.trim();
        }
        return tenantId;
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

    private String normalizeSignupAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return "HR";
        }

        String normalized = accountType.trim().toUpperCase();
        return "EMPLOYEE".equals(normalized) ? "EMPLOYEE" : "HR";
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
                employee.getBirthDate(),
                resolveGender(employee.getGender()),
                resolveContractType(employee.getContractType()),
                employee.isCrossBorderWorker(),
                employee.getHireDate(),
                employee.isActive(),
                employee.getRoles(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    private EmployeeInvitationResponse toInvitationResponse(EmployeeInvitation invitation, String token) {
        return toInvitationResponse(invitation, token, false);
    }

    private EmployeeInvitationResponse toInvitationResponse(EmployeeInvitation invitation, String token, boolean emailSent) {
        return new EmployeeInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getCountryOfResidence(),
                invitation.getPhoneNumber(),
                invitation.getDepartment(),
                invitation.getJobTitle(),
                invitation.getBirthDate(),
                resolveGender(invitation.getGender()),
                resolveContractType(invitation.getContractType()),
                invitation.getHireDate(),
                token,
                emailSent,
                resolveInvitationStatus(invitation),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getCreatedAt()
        );
    }

    private TenantWorkspaceResponse toWorkspaceResponse(TenantWorkspace workspace) {
        long seatsUsed = employeeRepository.countByTenantId(workspace.getTenantId());
        long activeEmployees = employeeRepository.countByTenantIdAndActiveTrue(workspace.getTenantId());
        Integer seatsPurchased = workspace.getSeatsPurchased();
        return new TenantWorkspaceResponse(
                workspace.getTenantId(),
                workspace.getCompanyName(),
                workspace.getOwnerEmail(),
                workspace.getPlanCode(),
                seatsPurchased,
                seatsUsed,
                activeEmployees,
                seatsPurchased != null && seatsUsed > seatsPurchased,
                workspace.isActive(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private PlatformWorkspaceResponse toPlatformWorkspaceResponse(TenantWorkspace workspace) {
        long seatsUsed = employeeRepository.countByTenantId(workspace.getTenantId());
        long activeEmployees = employeeRepository.countByTenantIdAndActiveTrue(workspace.getTenantId());
        Integer seatsPurchased = workspace.getSeatsPurchased();
        return new PlatformWorkspaceResponse(
                workspace.getTenantId(),
                workspace.getCompanyName(),
                workspace.getOwnerEmail(),
                workspace.getPlanCode(),
                seatsPurchased,
                seatsUsed,
                activeEmployees,
                seatsPurchased != null && seatsUsed > seatsPurchased,
                workspace.isActive(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private String resolveInvitationStatus(EmployeeInvitation invitation) {
        if (invitation.getAcceptedAt() != null) {
            return "ACCEPTED";
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(Instant.now())) {
            return "EXPIRED";
        }
        return "PENDING";
    }
}
