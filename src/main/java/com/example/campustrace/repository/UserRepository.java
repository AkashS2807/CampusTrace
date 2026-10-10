package com.example.campustrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.campustrace.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    User findByEmailIgnoreCase(String email);
}
