package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.entity.User;
import com.fitcoach.domain.enums.Role;
import com.fitcoach.dto.request.LoginRequest;
import com.fitcoach.dto.request.RegisterCoachRequest;
import com.fitcoach.dto.request.RegisterTraineeRequest;
import com.fitcoach.dto.response.AuthResponse;
import com.fitcoach.exception.ConflictException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final InvitationService invitationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

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

    // ── Helpers ───────────────────────────────────────────────────────────────
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
