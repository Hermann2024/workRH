package com.workrh.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "password_reset_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_password_reset_token_hash", columnNames = {"token_hash"})
})
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
