package com.pritam.saasbackend.permission.persistence;

import com.pritam.saasbackend.permission.domain.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pritam.saasbackend.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;


@IntegrationTest
class PermissionRepositoryIntegrationTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void shouldSaveAndFindPermission() {

        String code = "TEST_PERMISSION_" + UUID.randomUUID();

        Permission permission = new Permission(
                code,
                "Test permission"
        );

        Permission saved = permissionRepository.save(permission);

        UUID id = saved.getId();

        Permission found =
                permissionRepository.findById(id)
                        .orElseThrow();

        assertThat(found.getId()).isEqualTo(id);

        assertThat(found.getCode())
                .isEqualTo(code);

        assertThat(found.getDescription())
                .isEqualTo("Test permission");
    }

    @Test
    void shouldFindPermissionByCode() {

        String code = "TEST_PERMISSION_" + UUID.randomUUID();

        Permission permission = new Permission(
                code,
                "Test permission"
        );

        permissionRepository.save(permission);

        Optional<Permission> found =
                permissionRepository.findByCode(code);

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo(code);
    }
}