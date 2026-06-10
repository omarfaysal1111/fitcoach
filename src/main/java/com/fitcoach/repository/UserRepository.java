package com.fitcoach.repository;

import com.fitcoach.domain.entity.User;
import com.fitcoach.domain.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Look up an SSO user by their stable provider subject (Google/Apple {@code sub} claim).
     * Used on every SSO login so re-logins don't require a fresh email.
     */
    Optional<User> findByAuthProviderAndProviderSubject(AuthProvider provider, String providerSubject);
}
