package com.etd.account_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // length = 512 — JWT tokens are typically 300-500 chars
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    // kept so expired entries can be cleaned up automatically on startup
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

}
