package com.workrh.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.workrh.common.security.JwtService;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.users.api.dto.EmployeeCreateRequest;
import com.workrh.users.api.dto.EmployeeUpdateRequest;
import com.workrh.users.domain.Employee;
import com.workrh.users.domain.Role;
import com.workrh.users.repository.EmployeeRepository;
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
    private final EmployeeService employeeService = new EmployeeService(employeeRepository, passwordEncoder, jwtService);

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
                "jane@corp.com", "secret", "Jane", "Doe", "FR", "0600000000", "HR", "Manager", true,
                LocalDate.of(2025, 1, 1), Set.of(Role.HR)
        ));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.crossBorderWorker()).isTrue();
        assertThat(response.department()).isEqualTo("HR");
    }

    @Test
    void shouldRejectDuplicateEmailForTenant() {
        TenantContext.setTenantId("tenant-a");
        when(employeeRepository.existsByEmailAndTenantId("jane@corp.com", "tenant-a")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(new EmployeeCreateRequest(
                "jane@corp.com", "secret", "Jane", "Doe", "FR", null, null, null, true,
                LocalDate.of(2025, 1, 1), Set.of(Role.HR)
        ))).isInstanceOf(BadRequestException.class);
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
                "jane.doe@corp.com", "Jane", "Doe", "FR", "0600000000", "Finance", "HRBP", true,
                LocalDate.of(2025, 1, 1), Set.of(Role.ADMIN, Role.HR), true
        ));

        assertThat(response.email()).isEqualTo("jane.doe@corp.com");
        assertThat(response.roles()).contains(Role.ADMIN);
    }
}
