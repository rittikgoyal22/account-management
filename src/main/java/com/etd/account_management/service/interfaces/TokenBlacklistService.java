package com.etd.account_management.service.interfaces;

public interface TokenBlacklistService {

    void blacklistToken(String token);

    boolean isBlacklisted(String token);

    void deleteExpiredTokens();

}
