package com.pritam.saasbackend.tenant.api;

import com.pritam.saasbackend.tenant.api.dto.CreateTenantRequest;
import com.pritam.saasbackend.tenant.api.dto.TenantResponse;
import com.pritam.saasbackend.tenant.application.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse createTenant(
            @RequestBody CreateTenantRequest request
    ) {
        return tenantService.createTenant(request);
    }
}