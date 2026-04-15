// backend/src/main/java/com/sistema/lucas/config/AdminInitializer.java
package com.sistema.lucas.config;

import com.sistema.lucas.model.User;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@clinica.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.name:Administrador Principal}")
    private String adminName;

    @Override
    public void run(String... args) throws Exception {
        // Verifica se já existe algum administrador no sistema
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> user.getRole() == Role.ADMIN);

        if (!adminExists) {
            System.out.println("🛡️ Nenhum administrador encontrado. Criando Administrador Fundador...");

            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setVerified(true);
            
            userRepository.save(admin);
            System.out.println("✅ Administrador Fundador criado com sucesso! Email: " + adminEmail);
        } else {
            System.out.println("ℹ️ Administrador já existe na base de dados. Pulando a criação de admin...");
        }
    }
}
