package com.programming.auth.repository;

import com.programming.auth.entity.UserMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMfaRepository extends JpaRepository<UserMfa, UUID> {

    List<UserMfa> findByUserId(UUID userId);

    List<UserMfa> findByUserIdAndIsEnabledTrue(UUID userId);

    Optional<UserMfa> findByUserIdAndMfaType(UUID userId, UserMfa.MfaType mfaType);

    Optional<UserMfa> findByUserIdAndMfaTypeAndIsEnabledTrue(UUID userId, UserMfa.MfaType mfaType);

    List<UserMfa> findByMfaType(UserMfa.MfaType mfaType);

    List<UserMfa> findByTenantId(UUID tenantId);

    boolean existsByUserIdAndMfaType(UUID userId, UserMfa.MfaType mfaType);

    boolean existsByUserIdAndMfaTypeAndIsEnabledTrue(UUID userId, UserMfa.MfaType mfaType);

    @Query("SELECT um FROM UserMfa um WHERE um.userId = :userId AND um.isEnabled = true AND um.verifiedAt IS NOT NULL")
    List<UserMfa> findActiveVerifiedMfaByUserId(@Param("userId") UUID userId);

    @Query("SELECT um FROM UserMfa um WHERE um.phoneNumber = :phoneNumber AND um.mfaType = 'SMS' AND um.isEnabled = true")
    List<UserMfa> findByPhoneNumberAndEnabledTrue(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT um FROM UserMfa um WHERE um.email = :email AND um.mfaType = 'EMAIL' AND um.isEnabled = true")
    List<UserMfa> findByEmailAndEnabledTrue(@Param("email") String email);

    @Query("SELECT COUNT(um) FROM UserMfa um WHERE um.userId = :userId AND um.isEnabled = true")
    long countEnabledMfaByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(um) FROM UserMfa um WHERE um.tenantId = :tenantId AND um.isEnabled = true")
    long countEnabledMfaByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT um FROM UserMfa um WHERE um.userId = :userId AND um.mfaType = :mfaType AND um.verifiedAt IS NULL")
    Optional<UserMfa> findUnverifiedMfaByUserIdAndType(@Param("userId") UUID userId, @Param("mfaType") UserMfa.MfaType mfaType);
}