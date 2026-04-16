package com.workrh.telework.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "telework_declarations")
public class TeleworkDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private Long employeeId;
    private LocalDate workDate;
    private String countryCode;
    @Enumerated(EnumType.STRING)
    private TeleworkStatus status;
    private int totalWorkMinutes = 480;
    private int residenceTeleworkMinutes = 480;
    private int residenceNonTeleworkMinutes;
    private int otherForeignWorkMinutes;
    private String otherForeignCountryCode;
    private boolean connectedToEmployerInfrastructure = true;
    private int monthUsedDays;
    private int annualUsedDays;
    private int annualRemainingDays;
}
