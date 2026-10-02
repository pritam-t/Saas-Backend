package com.pritam.saasbackend.platformuser.persistence;

import com.pritam.saasbackend.platformuser.domain.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformUserRepository
        extends JpaRepository<PlatformUser, UUID> {

    Optional<PlatformUser> findByEmail(String email);

    boolean existsByEmail(String email);
}