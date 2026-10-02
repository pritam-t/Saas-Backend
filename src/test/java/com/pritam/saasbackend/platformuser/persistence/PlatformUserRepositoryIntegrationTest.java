package com.pritam.saasbackend.platformuser.persistence;

import com.pritam.saasbackend.platformuser.domain.PlatformUser;
import com.pritam.saasbackend.platformuser.domain.PlatformUserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PlatformUserRepositoryIntegrationTest {

    @Autowired
    private PlatformUserRepository platformUserRepository;

    @Test
    void shouldSaveAndFindPlatformUser() {

        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        String email = "repo-test-" + UUID.randomUUID() + "@example.com";


        PlatformUser user = new PlatformUser(
                id,
                email,
                "temporary-hash",
                PlatformUserStatus.ACTIVE,
                now,
                now
        );

        platformUserRepository.save(user);

        PlatformUser saved =
                platformUserRepository.findById(id)
                        .orElseThrow();

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getEmail())
                .isEqualTo(email);
        assertThat(saved.getPasswordHash())
                .isEqualTo("temporary-hash");
        assertThat(saved.getStatus())
                .isEqualTo(PlatformUserStatus.ACTIVE);

    }
}