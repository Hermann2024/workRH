package com.workrh.reporting.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hr_connector_configurations")
public class HrConnectorConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;

    @Enumerated(EnumType.STRING)
    private HrProvider provider;

    @Enumerated(EnumType.STRING)
    private ConnectorAuthType authType = ConnectorAuthType.EXPORT_ONLY;

    @Enumerated(EnumType.STRING)
    private ConnectorSyncMode syncMode = ConnectorSyncMode.EXPORT;

    @Enumerated(EnumType.STRING)
    private ConnectorStatus status = ConnectorStatus.NEEDS_CONFIGURATION;

    private boolean enabled;
    private String clientId;
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 1000)
    private String clientSecret;
    private String apiBaseUrl;
    private String authorizationUrl;
    private String tokenUrl;
    private String redirectUri;
    private String scopes;
    @Column(length = 2000)
    @Convert(converter = EncryptedStringConverter.class)
    private String accessToken;

    @Column(length = 2000)
    @Convert(converter = EncryptedStringConverter.class)
    private String refreshToken;
    private Instant tokenExpiresAt;
    private Instant lastSyncAt;
    private String lastSyncStatus;

    @Column(length = 1000)
    private String lastSyncMessage;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hr_connector_field_mappings", joinColumns = @JoinColumn(name = "configuration_id"))
    @MapKeyColumn(name = "workrh_field")
    @Column(name = "provider_field")
    private Map<String, String> fieldMappings = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hr_connector_api_settings", joinColumns = @JoinColumn(name = "configuration_id"))
    @MapKeyColumn(name = "setting_key")
    @Column(name = "setting_value")
    private Map<String, String> apiSettings = new HashMap<>();
}
