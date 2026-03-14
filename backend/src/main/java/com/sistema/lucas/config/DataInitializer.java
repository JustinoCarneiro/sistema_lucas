package com.sistema.lucas.config;

import com.sistema.lucas.model.*;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer implements CommandLineRunner {

    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // ✅ CORREÇÃO: só popula se o banco estiver completamente vazio
        if (userRepository.count() > 0) {
            System.out.println("ℹ️ Banco já populado. Pulando carga de demonstração.");
            return;
        }

        try {
            System.out.println("🌱 Carregando dados de demonstração...");

            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail("admin@clinica.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("✅ Admin criado: admin@clinica.com");

            Professional house = new Professional();
            house.setName("Dr. Gregory House");
            house.setEmail("house@clinica.com");
            house.setPassword(passwordEncoder.encode("123456"));
            house.setRole(Role.PROFESSIONAL);
            professionalRepository.save(house);

            Patient lucas = new Patient();
            lucas.setName("Lucas Silva");
            lucas.setEmail("lucas@email.com");
            lucas.setPassword(passwordEncoder.encode("123456"));
            lucas.setRole(Role.PATIENT);
            patientRepository.save(lucas);

            LocalDateTime hoje = LocalDateTime.now();
            appointmentRepository.save(new Appointment(house, lucas, hoje.minusDays(1), "Check-up", "COMPLETED"));
            appointmentRepository.save(new Appointment(house, lucas, hoje.withHour(14).withMinute(0), "Dor lombar", "SCHEDULED"));

            System.out.println("✅ Dados de demonstração carregados com sucesso!");
        } catch (Exception e) {
            System.err.println("⚠️ Erro na carga: " + e.getMessage());
        }
    }
}