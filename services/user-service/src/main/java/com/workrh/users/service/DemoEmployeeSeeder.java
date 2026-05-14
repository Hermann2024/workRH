package com.workrh.users.service;

import com.workrh.users.domain.Employee;
import com.workrh.users.domain.EmployeeGender;
import com.workrh.users.domain.EmploymentContractType;
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
                "Administrateur",
                LocalDate.of(1982, 3, 12),
                EmployeeGender.AUTRES,
                EmploymentContractType.CDI,
                Set.of(Role.ADMIN)
        );
        upsert(
                "rh@company.com",
                "Rhea",
                "Hubert",
                "LU",
                false,
                "Ressources humaines",
                "Responsable RH",
                LocalDate.of(1988, 9, 24),
                EmployeeGender.FEMININ,
                EmploymentContractType.CDI,
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
                LocalDate.of(1991, 5, 8),
                EmployeeGender.FEMININ,
                EmploymentContractType.CDI,
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
                LocalDate.of(1994, 2, 17),
                EmployeeGender.FEMININ,
                EmploymentContractType.CDD,
                Set.of(Role.EMPLOYEE)
        );
        upsert(
                "louis.cdi@company.com",
                "Louis",
                "Meyer",
                "FR",
                true,
                "Finance",
                "Analyste financier",
                LocalDate.of(1989, 11, 6),
                EmployeeGender.MASCULIN,
                EmploymentContractType.CDI,
                Set.of(Role.EMPLOYEE)
        );
        upsert(
                "claire.cdd@company.com",
                "Claire",
                "Martin",
                "BE",
                true,
                "Operations",
                "Coordinatrice operations",
                LocalDate.of(1992, 7, 21),
                EmployeeGender.FEMININ,
                EmploymentContractType.CDD,
                Set.of(Role.EMPLOYEE)
        );
        upsert(
                "nora.stage@company.com",
                "Nora",
                "Schmit",
                "FR",
                true,
                "Marketing",
                "Stagiaire marketing",
                LocalDate.of(2002, 1, 30),
                EmployeeGender.FEMININ,
                EmploymentContractType.STAGE,
                Set.of(Role.EMPLOYEE)
        );
        upsert(
                "yanis.alternance@company.com",
                "Yanis",
                "Klein",
                "DE",
                true,
                "IT",
                "Alternant support applicatif",
                LocalDate.of(2001, 6, 11),
                EmployeeGender.MASCULIN,
                EmploymentContractType.ALTERNANCE,
                Set.of(Role.EMPLOYEE)
        );
        upsert(
                "sara.autres@company.com",
                "Sara",
                "Dias",
                "LU",
                false,
                "Juridique",
                "Consultante externe",
                LocalDate.of(1990, 12, 3),
                EmployeeGender.AUTRES,
                EmploymentContractType.AUTRES,
                Set.of(Role.EMPLOYEE)
        );
        upsert(
                "mathis.stage@company.com",
                "Mathis",
                "Bernard",
                "FR",
                true,
                "Ressources humaines",
                "Stagiaire RH",
                LocalDate.of(2003, 4, 18),
                EmployeeGender.MASCULIN,
                EmploymentContractType.STAGE,
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
            LocalDate birthDate,
            EmployeeGender gender,
            EmploymentContractType contractType,
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
        employee.setBirthDate(birthDate);
        employee.setGender(gender);
        employee.setContractType(contractType);
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
