package com.pritam.saasbackend.platformuser.persistence;

import com.pritam.saasbackend.platformuser.application.PlatformUserService;
import com.pritam.saasbackend.platformuser.domain.PlatformUser;
import com.pritam.saasbackend.platformuser.persistence.PlatformUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pritam.saasbackend.support.IntegrationTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@IntegrationTest
class PlatformUserServiceIntegrationTest {

    @Autowired
    private PlatformUserService platformUserService;

    @Autowired
    private PlatformUserRepository platformUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldHashPasswordBeforeSaving() {

        String email =
                "bcrypt-test-" + UUID.randomUUID() + "@example.com";
        String rawPassword = "my-password";

        PlatformUser user =
                platformUserService.createUser(
                        email,
                        rawPassword
                );

        assertThat(user.getPasswordHash())
                .isNotEqualTo(rawPassword);

        assertThat(
                passwordEncoder.matches(
                        rawPassword,
                        user.getPasswordHash()
                )
        ).isTrue();

        assertThat(
                platformUserRepository.findById(user.getId())
        ).isPresent();
    }
}