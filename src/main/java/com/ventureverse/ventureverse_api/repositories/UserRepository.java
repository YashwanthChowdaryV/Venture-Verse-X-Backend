package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Existing methods
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // NEW: Username support
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    // NEW: Login with email OR username
    @Query("SELECT u FROM User u WHERE u.email = :login OR u.username = :login")
    Optional<User> findByEmailOrUsername(@Param("login") String login);

    // NEW: Check if email or username exists (for validation)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :login OR u.username = :login")
    boolean existsByEmailOrUsername(@Param("login") String login);

    // Optional: Find by exact email and username for specific checks
    Optional<User> findByEmailAndUsername(String email, String username);
}