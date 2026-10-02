//package com.pritam.saasbackend.tenant.persistence;
//
//import com.pritam.saasbackend.tenant.domain.Tenant;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//class TenantRepositoryIntegrationTest {
//
//    @Autowired
//    private TenantRepository tenantRepository;
//
//    @Test
//    void shouldSaveAndFindTenant() {
//
//        String schemaName =
//                "tenant_" + System.currentTimeMillis();
//
//        Tenant tenant = new Tenant(
//                "Test Tenant",
//                schemaName
//        );
//
//        Tenant saved =
//                tenantRepository.save(tenant);
//
//        assertThat(saved.getId()).isNotNull();
//
//        Optional<Tenant> found =
//                tenantRepository.findBySchemaName(schemaName);
//
//        assertThat(found).isPresent();
//
//        assertThat(found.get().getId())
//                .isEqualTo(saved.getId());
//
//        assertThat(found.get().getName())
//                .isEqualTo("Test Tenant");
//
//        assertThat(found.get().getSchemaName())
//                .isEqualTo(schemaName);
//    }
//}