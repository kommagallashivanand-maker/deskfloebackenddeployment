package com.p99soft.deskflow.repository;

import com.p99soft.deskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
