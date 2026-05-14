package com.workrh.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workrh.common.security.JwtService;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.users.api.dto.EmployeeCreateRequest;
import com.workrh.users.api.dto.EmployeeUpdateRequest;
import com.workrh.users.api.dto.PasswordResetConfirmRequest;
import com.workrh.users.api.dto.SignupRequest;
import com.workrh.users.domain.Employee;
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
import java.util.HexFormat;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class EmployeeServiceTest {

    private final EmployeeRepository employeeRepository = Mockito.mock(EmployeeRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private final JwtService jwtService = Mockito.mock(JwtService.class);
    private final SubscriptionBootstrapClient subscriptionBootstrapClient =
            Mockito.mock(SubscriptionBootstrapClient.class);
    private final TenantWorkspaceRepository tenantWorkspaceRepository = Mockito.mock(TenantWorkspaceRepository.class);
    private final EmployeeInvitationRepository employeeInvitationRepository = Mockito.mock(EmployeeInvitationRepository.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
    private final EmployeeInvitationNotificationClient invitationNotificationClient =
            Mockito.mock(EmployeeInvitationNotificationClient.class);
    private final EmployeeService employeeService = new EmployeeService(
            employeeRepository,
            passwordEncoder,
            jwtService,
            subscriptionBootstrapClient,
            tenantWorkspaceRepository,
            employeeInvitationRepository,
            passwordResetTokenRepository,
            invitationNotificationClient
    );

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateEmployeeForTenant() {
        TenantContext.setTenantId("tenant-a");
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(employeeRepository.existsByEmailAndTenantId("jane@corp.com", "tenant-a")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(1L);
            return employee;
        });

        var response = employeeService.create(new EmployeeCreateRequest(
                "jane@corp.com", "secret", "Jane", "Doe", "FR", "0600000000", "HR", "Manager",
                LocalDate.of(1990, 2, 3), EmployeeGender.FEMININ, EmploymentContractType.CDI, true,
                LocalDate.of(2025, 1, 1), Set.of(Role.HR)
        ));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.crossBorderWorker()).isTrue();
        assertThat(response.department()).isEqualTo("HR");
        assertThat(response.gender()).isEqualTo(EmployeeGender.FEMININ);
        assertThat(response.contractType()).isEqualTo(EmploymentContractType.CDI);
    }

    @Test
    void shouldRejectDuplicateEmailForTenant() {
        TenantContext.setTenantId("tenant-a");
        when(employeeRepository.existsByEmailAndTenantId("jane@corp.com", "tenant-a")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(new EmployeeCreateRequest(
                "jane@corp.com", "secret", "Jane", "Doe", "FR", null, null, null,
                null, EmployeeGender.AUTRES, EmploymentContractType.CDI, true,
                LocalDate.of(2025, 1, 1), Set.of(Role.HR)
        ))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectEmployeeCreationWhenSeatLimitReached() {
        TenantContext.setTenantId("tenant-a");
        TenantWorkspace workspace = new TenantWorkspace();
        workspace.setTenantId("tenant-a");
        workspace.setCompanyName("Tenant A");
        workspace.setSeatsPurchased(1);

        when(employeeRepository.existsByEmailAndTenantId("jane@corp.com", "tenant-a")).thenReturn(false);
        when(tenantWorkspaceRepository.findById("tenant-a")).thenReturn(Optional.of(workspace));
        when(employeeRepository.countByTenantId("tenant-a")).thenReturn(1L);

        assertThatThrownBy(() -> employeeService.create(new EmployeeCreateRequest(
                "jane@corp.com", "secret", "Jane", "Doe", "FR", null, null, null,
                null, EmployeeGender.AUTRES, EmploymentContractType.CDI, true,
                LocalDate.of(2025, 1, 1), Set.of(Role.EMPLOYEE)
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Seat limit reached");
    }

    @Test
    void shouldUpdateEmployee() {
        TenantContext.setTenantId("tenant-a");
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setTenantId("tenant-a");
        employee.setEmail("jane@corp.com");
        employee.setPassword("hashed");
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setRoles(Set.of(Role.HR));
        employee.setCreatedAt(Instant.now());

        when(employeeRepository.findByIdAndTenantId(1L, "tenant-a")).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndTenantIdAndIdNot("jane.doe@corp.com", "tenant-a", 1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = employeeService.update(1L, new EmployeeUpdateRequest(
                "jane.doe@corp.com", "Jane", "Doe", "FR", "0600000000", "Finance", "HRBP",
                LocalDate.of(1990, 2, 3), EmployeeGender.FEMININ, EmploymentContractType.CDD, true,
                LocalDate.of(2025, 1, 1), Set.of(Role.ADMIN, Role.HR), true
        ));

        assertThat(response.email()).isEqualTo("jane.doe@corp.com");
        assertThat(response.contractType()).isEqualTo(EmploymentContractType.CDD);
        assertThat(response.roles()).contains(Role.ADMIN);
    }

    @Test
    void shouldBootstrapRequestedTrialPlanDuringSignup() {
        TenantContext.setTenantId("tenant-pro");
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(employeeRepository.existsByTenantId("tenant-pro")).thenReturn(false);
        when(employeeRepository.existsByEmailAndTenantId("owner@corp.com", "tenant-pro")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(7L);
            return employee;
        });
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("token");

        var response = employeeService.signup(new SignupRequest(
                null,
                null,
                "Owner",
                "Admin",
                "owner@corp.com",
                "secret123",
                12,
                "HR",
                "PRO"
        ));

        assertThat(response.tenantId()).isEqualTo("tenant-pro");
        verify(subscriptionBootstrapClient).initializeTrial("tenant-pro", "owner@corp.com", 12, "PRO");
    }

    @Test
    void shouldFallbackToStarterTrialForEnterpriseSignup() {
        TenantContext.setTenantId("tenant-enterprise");
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(employeeRepository.existsByTenantId("tenant-enterprise")).thenReturn(false);
        when(employeeRepository.existsByEmailAndTenantId("owner@corp.com", "tenant-enterprise")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(8L);
            return employee;
        });
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("token");

        employeeService.signup(new SignupRequest(
                null,
                null,
                "Owner",
                "Admin",
                "owner@corp.com",
                "secret123",
                5,
                "HR",
                "ENTERPRISE"
        ));

        verify(subscriptionBootstrapClient).initializeTrial("tenant-enterprise", "owner@corp.com", 5, "STARTER");
    }

    @Test
    void shouldCreateEmployeeAccountWithoutSubscriptionBootstrapDuringSignup() {
        TenantContext.setTenantId("tenant-a");
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(employeeRepository.existsByTenantId("tenant-a")).thenReturn(true);
        when(employeeRepository.existsByEmailAndTenantId("employee@corp.com", "tenant-a")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(9L);
            return employee;
        });
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("token");

        var response = employeeService.signup(new SignupRequest(
                null,
                null,
                "Jane",
                "Employee",
                "employee@corp.com",
                "secret123",
                null,
                "EMPLOYEE",
                null
        ));

        assertThat(response.roles()).containsExactly("EMPLOYEE");
        verify(subscriptionBootstrapClient, never()).initializeTrial(any(), any(), anyInt(), any());
    }

    @Test
    void shouldConfirmPasswordResetAndConsumeToken() throws Exception {
        String token = "reset-token";
        String tokenHash = hashToken(token);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTenantId("tenant-a");
        resetToken.setEmail("jane@corp.com");
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plusSeconds(900));

        Employee employee = new Employee();
        employee.setId(42L);
        employee.setTenantId("tenant-a");
        employee.setEmail("jane@corp.com");
        employee.setPassword("old");
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setActive(true);
        employee.setRoles(Set.of(Role.EMPLOYEE));

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(employeeRepository.findByEmailAndTenantId("jane@corp.com", "tenant-a")).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("newsecret")).thenReturn("new-hash");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("token");

        var response = employeeService.confirmPasswordReset(new PasswordResetConfirmRequest(token, "newsecret"));

        assertThat(response.roles()).containsExactly("EMPLOYEE");
        assertThat(employee.getPassword()).isEqualTo("new-hash");
        assertThat(resetToken.getUsedAt()).isNotNull();
    }

    private String hashToken(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}
