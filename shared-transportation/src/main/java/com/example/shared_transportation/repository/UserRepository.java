package com.example.shared_transportation.repository;

import com.example.shared_transportation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByRole(String role);

    long countByCreatedAtAfter(LocalDateTime time);

    List<User> findAllByOrderByCreatedAtDesc();
}
