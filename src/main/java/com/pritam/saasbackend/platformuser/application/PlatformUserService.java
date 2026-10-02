package com.pritam.saasbackend.platformuser.application;

import com.pritam.saasbackend.platformuser.domain.PlatformUser;
import com.pritam.saasbackend.platformuser.domain.PlatformUserStatus;
import com.pritam.saasbackend.platformuser.persistence.PlatformUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PlatformUserService {

    private final PlatformUserRepository platformUserRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformUserService(
            PlatformUserRepository platformUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.platformUserRepository = platformUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PlatformUser createUser(
            String email,
            String rawPassword
    ) {
        if (platformUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Platform user already exists: " + email
            );
        }

        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        String passwordHash =
                passwordEncoder.encode(rawPassword);

        PlatformUser user = new PlatformUser(
                id,
                email,
                passwordHash,
                PlatformUserStatus.ACTIVE,
                now,
                now
        );

        return platformUserRepository.save(user);
    }
}