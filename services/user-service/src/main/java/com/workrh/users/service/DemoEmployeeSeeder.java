package com.workrh.users.service;

import com.workrh.users.domain.Employee;
import com.workrh.users.domain.Role;
import com.workrh.users.repository.EmployeeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class DemoEmployeeSeeder implements CommandLineRunner {

    private static final String TENANT_ID = "demo-lu";
    private static final String DEFAULT_PASSWORD = "secret";

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoEmployeeSeeder(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        upsert(
                "admin@company.com",
                "Admin",
                "WorkRH",
                "LU",
                false,
                "Direction",
                "Administrateur plateforme",
                Set.of(Role.ADMIN, Role.HR)
        );
        upsert(
                "rh@company.com",
                "Rhea",
                "Hubert",
                "LU",
                false,
                "Ressources humaines",
                "Responsable RH",
                Set.of(Role.HR)
        );
        upsert(
                "demo@company.com",
                "Dora",
                "Demo",
                "LU",
                false,
                "Demonstration",
                "Compte de demonstration",
                Set.of(Role.HR)
        );
        upsert(
                "employee@company.com",
                "Emma",
                "Frontaliere",
                "FR",
                true,
                "Finance",
                "Chargee de mission",
                Set.of(Role.EMPLOYEE)
        );
    }

    private void upsert(
            String email,
            String firstName,
            String lastName,
            String countryOfResidence,
            boolean crossBorderWorker,
            String department,
            String jobTitle,
            Set<Role> roles) {
        Employee employee = employeeRepository.findByEmailAndTenantId(email, TENANT_ID).orElseGet(Employee::new);
        employee.setTenantId(TENANT_ID);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setCountryOfResidence(countryOfResidence);
        employee.setPhoneNumber("+352 27 00 00 00");
        employee.setDepartment(department);
        employee.setJobTitle(jobTitle);
        employee.setCrossBorderWorker(crossBorderWorker);
        employee.setHireDate(LocalDate.of(2024, 1, 15));
        employee.setActive(true);
        employee.setRoles(roles);
        if (employee.getCreatedAt() == null) {
            employee.setCreatedAt(Instant.now());
        }
        employee.setUpdatedAt(Instant.now());
        employeeRepository.save(employee);
    }
}
