package com.fitcoach.repository;

import com.fitcoach.domain.entity.User;
import com.fitcoach.domain.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Look up an SSO user by their stable provider subject (Google/Apple {@code sub} claim).
     * Used on every SSO login so re-logins don't require a fresh email.
     */
    Optional<User> findByAuthProviderAndProviderSubject(AuthProvider provider, String providerSubject);
}
