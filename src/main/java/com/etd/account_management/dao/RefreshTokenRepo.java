package com.etd.account_management.dao;

import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // @Modifying executes a direct DELETE SQL immediately — avoids Hibernate batching
    // the delete behind the next INSERT (which causes unique constraint violation on employee_id)
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken rt WHERE rt.employee = :employee")
    void deleteByEmployee(@Param("employee") Employee employee);

}
