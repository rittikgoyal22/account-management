package com.etd.account_management.dao;

import com.etd.account_management.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TokenBlacklistRepo extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByToken(String token);

    // bulk DELETE so expired entries are cleaned up efficiently on startup
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

}
