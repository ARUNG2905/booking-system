package com.undoschool.bookingsystem.service;

import com.undoschool.bookingsystem.dto.request.AuthRequests.LoginRequest;
import com.undoschool.bookingsystem.dto.request.AuthRequests.RegisterRequest;
import com.undoschool.bookingsystem.dto.response.Responses.AuthResponse;
import com.undoschool.bookingsystem.entity.User;
import com.undoschool.bookingsystem.exception.BookingExceptions.EmailAlreadyExistsException;
import com.undoschool.bookingsystem.exception.BookingExceptions.InvalidTimezoneException;
import com.undoschool.bookingsystem.repository.UserRepository;
import com.undoschool.bookingsystem.security.JwtUtil;
import com.undoschool.bookingsystem.util.TimezoneConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TimezoneConverter timezoneConverter;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        if (!timezoneConverter.isValidTimezone(request.getTimezone())) {
            throw new InvalidTimezoneException(request.getTimezone());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .timezone(request.getTimezone())
                .build();

        userRepository.save(user);
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .build();
    }
}
