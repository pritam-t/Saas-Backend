package com.pritam.saasbackend.auth.application;

import com.pritam.saasbackend.auth.api.LoginRequest;
import com.pritam.saasbackend.auth.api.LoginResponse;
import com.pritam.saasbackend.platformuser.domain.PlatformUser;
import com.pritam.saasbackend.platformuser.persistence.PlatformUserRepository;
import com.pritam.saasbackend.security.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final PlatformUserRepository platformUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            PlatformUserRepository platformUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.platformUserRepository = platformUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        PlatformUser user = platformUserRepository
                .findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                "PLATFORM_USER"
        );

        return new LoginResponse(accessToken);
    }
}