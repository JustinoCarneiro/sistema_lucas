package com.sistema.lucas.repository;

import com.sistema.lucas.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // O Spring Security precisa que retornemos um UserDetails pelo email (username)
    UserDetails findByEmail(String email);
}