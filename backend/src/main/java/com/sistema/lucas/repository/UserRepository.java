package com.sistema.lucas.repository;

import com.sistema.lucas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    
    // Altere de UserDetails para User AQUI:
    User findByEmail(String email); 
}