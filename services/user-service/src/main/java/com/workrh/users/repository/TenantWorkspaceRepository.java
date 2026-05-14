package com.workrh.users.repository;

import com.workrh.users.domain.TenantWorkspace;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TenantWorkspaceRepository extends JpaRepository<TenantWorkspace, String> {
    @Modifying
    @Query(value = """
            INSERT INTO tenant_workspaces (
                tenant_id,
                company_name,
                owner_email,
                plan_code,
                seats_purchased,
                active,
                created_at,
                updated_at
            ) VALUES (
                :tenantId,
                :companyName,
                :ownerEmail,
                :planCode,
                :seatsPurchased,
                :active,
                :now,
                :now
            )
            ON CONFLICT (tenant_id) DO UPDATE SET
                company_name = CASE
                    WHEN tenant_workspaces.company_name = tenant_workspaces.tenant_id THEN EXCLUDED.company_name
                    ELSE tenant_workspaces.company_name
                END,
                owner_email = COALESCE(EXCLUDED.owner_email, tenant_workspaces.owner_email),
                plan_code = EXCLUDED.plan_code,
                seats_purchased = EXCLUDED.seats_purchased,
                active = EXCLUDED.active,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsertSubscriptionFields(
            String tenantId,
            String companyName,
            String ownerEmail,
            String planCode,
            int seatsPurchased,
            boolean active,
            Instant now);
}
