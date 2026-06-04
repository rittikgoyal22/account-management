package com.etd.account_management.controller;

import com.etd.account_management.dto.AuthRequestDTO;
import com.etd.account_management.dto.AuthResponseDTO;
import com.etd.account_management.exception.BadRequestException;
import com.etd.account_management.util.JWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

import static com.etd.account_management.constant.AppConstant.EMAIL_ADDRESS;
import static com.etd.account_management.constant.AppConstant.ERROR_INVALID_CREDENTIALS;

@RestController
@CrossOrigin
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final MessageSource messageSource;

    public AuthController(AuthenticationManager authenticationManager,
                          JWTUtil jwtUtil,
                          MessageSource messageSource) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.messageSource = messageSource;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        logger.info("Inside AuthController :: Login attempt for: {}", request.getEmailAddress());
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmailAddress(), request.getPassword()));
            String role = auth.getAuthorities().iterator().next().getAuthority();
            String token = jwtUtil.generateToken(request.getEmailAddress());
            return ResponseEntity.ok(AuthResponseDTO.builder()
                    .token(token)
                    .emailAddress(request.getEmailAddress())
                    .role(role)
                    .build());
        } catch (BadCredentialsException e) {
            logger.warn("Bad credentials for: {}", request.getEmailAddress());
            throw new BadRequestException(
                    messageSource.getMessage(ERROR_INVALID_CREDENTIALS, null, Locale.ENGLISH),
                    EMAIL_ADDRESS);
        }
    }

}
