package com.radai.repository;

import com.radai.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByEmail(String email);
    boolean existsByUsername(String username);
}

