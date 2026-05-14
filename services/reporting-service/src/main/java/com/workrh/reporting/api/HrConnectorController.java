package com.workrh.reporting.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.reporting.api.dto.ConnectorAuthorizationResponse;
import com.workrh.reporting.api.dto.ConnectorCatalogItem;
import com.workrh.reporting.api.dto.ConnectorConfigurationRequest;
import com.workrh.reporting.api.dto.ConnectorConfigurationResponse;
import com.workrh.reporting.api.dto.ConnectorSyncResponse;
import com.workrh.reporting.domain.HrProvider;
import com.workrh.reporting.service.HrConnectorService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/connectors")
public class HrConnectorController {

    private final HrConnectorService hrConnectorService;

    public HrConnectorController(HrConnectorService hrConnectorService) {
        this.hrConnectorService = hrConnectorService;
    }

    @GetMapping("/catalog")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EXPORTS)
    public List<ConnectorCatalogItem> catalog() {
        return hrConnectorService.catalog();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EXPORTS)
    public List<ConnectorConfigurationResponse> configurations() {
        return hrConnectorService.configurations();
    }

    @PutMapping("/{provider}")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EXPORTS)
    public ConnectorConfigurationResponse configure(
            @PathVariable HrProvider provider,
            @RequestBody ConnectorConfigurationRequest request
    ) {
        return hrConnectorService.configure(provider, request);
    }

    @PostMapping("/{provider}/authorize")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EXPORTS)
    public ConnectorAuthorizationResponse authorize(@PathVariable HrProvider provider) {
        return hrConnectorService.authorizationUrl(provider);
    }

    @PostMapping("/{provider}/sync")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EXPORTS)
    public ConnectorSyncResponse sync(@PathVariable HrProvider provider) {
        return hrConnectorService.sync(provider);
    }
}
