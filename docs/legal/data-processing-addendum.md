# WorkRH Data Processing Addendum

Last updated: 2026-05-14

This DPA template applies when WorkRH processes personal data for a customer in connection with the WorkRH SaaS platform.

## Roles

Customer: controller.

WorkRH: processor for HR workspace data processed on the customer's behalf.

## Processing Scope

Processing includes hosting, storing, retrieving, calculating, displaying, exporting and supporting HR, telework, leave, sickness, compliance and subscription-related data.

## Categories of Data Subjects

- employees;
- HR administrators;
- platform administrators;
- support contacts;
- billing contacts.

## Categories of Personal Data

- identification and contact data;
- employment and organisational data;
- leave, sickness and telework declarations;
- compliance status, thresholds and audit trail;
- support and technical diagnostics;
- billing references and subscription metadata.

## Processor Obligations

WorkRH must:

- process personal data only on documented customer instructions;
- ensure confidentiality for authorised personnel;
- apply appropriate technical and organisational measures;
- assist with data subject rights where applicable;
- assist with security incident investigation and notification;
- delete or return personal data at the end of service, subject to legal retention;
- make available information needed to demonstrate compliance.

## Security Measures

Baseline measures include:

- tenant isolation through token tenant claims and tenant context;
- role-based access control;
- internal service keys for inter-service calls;
- production readiness guards for unsafe configuration;
- encrypted connector secrets;
- audit trails for sensitive workflows;
- backups and restore process;
- health checks, smoke tests and incident runbook.

## Subprocessors

WorkRH must maintain an active subprocessor list before production launch. Typical subprocessors may include hosting, email, SMS, payment and monitoring providers.

## International Transfers

Transfers outside the EEA require documented safeguards.

## Incident Notification

WorkRH should notify affected customers without undue delay after becoming aware of a confirmed personal data breach.

This document is a production template and must be reviewed by legal counsel before public launch.
