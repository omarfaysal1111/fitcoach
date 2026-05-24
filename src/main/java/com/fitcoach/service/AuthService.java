package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.entity.User;
import com.fitcoach.domain.enums.AuthProvider;
import com.fitcoach.domain.enums.Role;
import com.fitcoach.dto.request.AppleAuthRequest;
import com.fitcoach.dto.request.GoogleAuthRequest;
import com.fitcoach.dto.request.LoginRequest;
import com.fitcoach.dto.request.RegisterCoachRequest;
import com.fitcoach.dto.request.RegisterTraineeRequest;
import com.fitcoach.dto.response.AuthResponse;
import com.fitcoach.exception.BadRequestException;
import com.fitcoach.exception.ConflictException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final InvitationService invitationService;
    private final SubscriptionService subscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;

    // ── Coach Registration ────────────────────────────────────────────────────
    @Transactional
    public AuthResponse registerCoach(RegisterCoachRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.COACH)
                .build();
        userRepository.save(user);

        Coach coach = Coach.builder()
                .user(user)
                .bio(request.getBio())
                .specialisation(request.getSpecialisation())
                .build();
        coachRepository.save(coach);

        // Provision a 7-day trial subscription for the new coach
        subscriptionService.initTrial(coach);

        long iatSec = Instant.now().getEpochSecond();
        user.setJwtIssuedEpochSec(iatSec);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), iatSec);
        return buildAuthResponse(token, user);
    }

    // ── Trainee Registration (invitation required) ───────────────────────────
    @Transactional
    public AuthResponse registerTrainee(RegisterTraineeRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use: " + request.getEmail());
        }

        // Validate and consume invitation
        Coach coach = invitationService.consumeInvitation(request.getInvitationToken(), request.getEmail());

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.TRAINEE)
                .build();
        userRepository.save(user);

        Trainee trainee = Trainee.builder()
                .user(user)
                .coach(coach)
                .fitnessGoal(request.getFitnessGoal())
                .build();
        traineeRepository.save(trainee);

        long iatSec = Instant.now().getEpochSecond();
        user.setJwtIssuedEpochSec(iatSec);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), iatSec);
        return buildAuthResponse(token, user);
    }

    // ── Login (both roles) ────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        long iatSec = Instant.now().getEpochSecond();
        user.setJwtIssuedEpochSec(iatSec);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), iatSec);
        return buildAuthResponse(token, user);
    }

    // ── Google SSO ────────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());

        String email   = payload.getEmail();
        String name    = (String) payload.get("name");   // may be null
        String subject = payload.getSubject();           // stable Google user ID

        return handleSsoLogin(email, name, subject,
                AuthProvider.GOOGLE, request.getRole(), request.getInvitationToken());
    }

    // ── Apple SSO ─────────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse loginWithApple(AppleAuthRequest request) {
        Claims claims = appleTokenVerifier.verify(request.getIdentityToken());

        // Apple omits email on re-logins; subject is always present
        String email   = claims.get("email", String.class);
        String subject = claims.getSubject();

        return handleSsoLogin(email, request.getFullName(), subject,
                AuthProvider.APPLE, request.getRole(), request.getInvitationToken());
    }

    // ── Shared SSO logic ──────────────────────────────────────────────────────
    private AuthResponse handleSsoLogin(
            String email,
            String fullName,
            String providerSubject,
            AuthProvider provider,
            String roleStr,
            String invitationToken) {

        // 1. Try to find by provider subject (most stable — Apple can hide email on re-login)
        User user = userRepository
                .findByAuthProviderAndProviderSubject(provider, providerSubject)
                .orElse(null);

        // 2. If not found by subject, try by email (handles first-time login where subject unknown)
        if (user == null && email != null) {
            user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                if (user.getAuthProvider() == AuthProvider.LOCAL) {
                    // Email belongs to a password account — reject SSO
                    throw new ConflictException(
                            "An account with this email already exists. Please log in with your password.");
                }
                // Same SSO provider, missing subject — backfill it
                user.setProviderSubject(providerSubject);
            }
        }

        if (user != null) {
            // ── RE-LOGIN: just issue a fresh JWT ──────────────────────────────
            long iatSec = Instant.now().getEpochSecond();
            user.setJwtIssuedEpochSec(iatSec);
            userRepository.save(user);
            String token = jwtUtil.generateToken(user.getEmail(), iatSec);
            return buildAuthResponse(token, user);
        }

        // ── FIRST-TIME REGISTRATION ───────────────────────────────────────────
        if (email == null) {
            throw new BadRequestException(
                    "Cannot create account: email was not returned by the identity provider. " +
                    "Please allow email access when signing in.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(
                    "An account with this email already exists. Please log in with your password.");
        }

        Role role = parseRole(roleStr);

        User newUser = User.builder()
                .fullName(fullName != null && !fullName.isBlank() ? fullName : email.split("@")[0])
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // unusable random password
                .role(role)
                .authProvider(provider)
                .providerSubject(providerSubject)
                .build();
        userRepository.save(newUser);

        if (role == Role.COACH) {
            Coach coach = Coach.builder().user(newUser).build();
            coachRepository.save(coach);
            subscriptionService.initTrial(coach);   // 7-day trial

        } else { // TRAINEE — invitation still required for first-time registration
            if (invitationToken == null || invitationToken.isBlank()) {
                throw new BadRequestException(
                        "Trainees must provide an invitationToken to create an account via SSO.");
            }
            Coach coach = invitationService.consumeInvitation(UUID.fromString(invitationToken), email);
            Trainee trainee = Trainee.builder()
                    .user(newUser)
                    .coach(coach)
                    .build();
            traineeRepository.save(trainee);
        }

        long iatSec = Instant.now().getEpochSecond();
        newUser.setJwtIssuedEpochSec(iatSec);
        userRepository.save(newUser);
        String token = jwtUtil.generateToken(newUser.getEmail(), iatSec);
        return buildAuthResponse(token, newUser);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Role parseRole(String roleStr) {
        return switch (roleStr.toLowerCase()) {
            case "coach"   -> Role.COACH;
            case "trainee" -> Role.TRAINEE;
            default        -> throw new IllegalArgumentException("Unknown role: " + roleStr);
        };
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
