package com.etd.account_management.controller;

import com.etd.account_management.dto.AuthRequestDTO;
import com.etd.account_management.dto.AuthResponseDTO;
import com.etd.account_management.dto.RefreshRequestDTO;
import com.etd.account_management.entity.RefreshToken;
import com.etd.account_management.exception.BadRequestException;
import com.etd.account_management.service.interfaces.RefreshTokenService;
import com.etd.account_management.service.interfaces.TokenBlacklistService;
import com.etd.account_management.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final MessageSource messageSource;

    public AuthController(AuthenticationManager authenticationManager,
                          JWTUtil jwtUtil,
                          RefreshTokenService refreshTokenService,
                          TokenBlacklistService tokenBlacklistService,
                          MessageSource messageSource) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.messageSource = messageSource;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        logger.info("Inside AuthController :: Login attempt for: {}", request.getEmailAddress());
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmailAddress(), request.getPassword()));

            String email = auth.getName();
            String role = auth.getAuthorities().iterator().next().getAuthority();

            String accessToken = jwtUtil.generateToken(email);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);

            return ResponseEntity.ok(AuthResponseDTO.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .emailAddress(email)
                    .role(role)
                    .build());

        } catch (BadCredentialsException e) {
            logger.warn("Bad credentials for: {}", request.getEmailAddress());
            throw new BadRequestException(
                    messageSource.getMessage(ERROR_INVALID_CREDENTIALS, null, Locale.ENGLISH),
                    EMAIL_ADDRESS);
        }
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody RefreshRequestDTO request) {
        logger.info("Inside AuthController :: Token refresh attempt");

        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        String email = refreshToken.getEmployee().getEmailAddress();
        String role = refreshToken.getEmployee().getRole();

        // rotate: old refresh token is deleted inside createRefreshToken, new one is issued
        String newAccessToken = jwtUtil.generateToken(email);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(email);

        logger.info("Inside AuthController :: Token refreshed successfully for: {}", email);

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .emailAddress(email)
                .role(role)
                .build());
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequestDTO request,
                                       HttpServletRequest httpRequest) {
        logger.info("Inside AuthController :: Logout attempt");

        // 1. delete refresh token from DB
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        refreshTokenService.deleteByEmailAddress(refreshToken.getEmployee().getEmailAddress());

        // 2. blacklist the access token so it cannot be used even within its remaining 1-hour window
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(accessToken);
        }

        logger.info("Inside AuthController :: Logout successful");
        return ResponseEntity.noContent().build();
    }

}
