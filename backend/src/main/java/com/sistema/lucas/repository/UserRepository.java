package com.sistema.lucas.repository;

import com.sistema.lucas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // O Spring Security usa UserDetails para a autenticação
    UserDetails findByEmail(String email);
}