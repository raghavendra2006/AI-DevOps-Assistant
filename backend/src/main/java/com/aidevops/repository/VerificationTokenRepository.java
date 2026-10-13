package com.aidevops.repository;

import com.aidevops.domain.TokenType;
import com.aidevops.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByTokenHashAndTokenType(String tokenHash, TokenType tokenType);
}
