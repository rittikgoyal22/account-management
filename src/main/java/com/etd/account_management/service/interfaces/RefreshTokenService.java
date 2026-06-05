package com.etd.account_management.service.interfaces;

import com.etd.account_management.entity.RefreshToken;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(String emailAddress);

    RefreshToken verifyExpiration(RefreshToken refreshToken);

    void deleteByEmailAddress(String emailAddress);

    RefreshToken findByToken(String token);

}
