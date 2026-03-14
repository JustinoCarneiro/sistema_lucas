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
        try {
            System.out.println("🧹 LIMPANDO BANCO PARA CARGA DE DEMONSTRAÇÃO...");
            appointmentRepository.deleteAll();
            professionalRepository.deleteAll();
            patientRepository.deleteAll();
            userRepository.deleteAll();

            // 1. Criar ADMIN (Acesso mestre ao sistema)
            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail("admin@clinica.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("✅ Admin criado: admin@clinica.com");

            // 2. Criar Médicos
            Professional house = new Professional();
            house.setName("Dr. Gregory House");
            house.setEmail("house@clinica.com");
            house.setPassword(passwordEncoder.encode("123456"));
            house.setRole(Role.PROFESSIONAL);
            professionalRepository.save(house);

            // 3. Criar Pacientes
            Patient lucas = new Patient();
            lucas.setName("Lucas Silva");
            lucas.setEmail("lucas@email.com");
            lucas.setPassword(passwordEncoder.encode("123456"));
            lucas.setRole(Role.PATIENT);
            patientRepository.save(lucas);

            // 4. Agenda do Dr. House (Demonstração)
            LocalDateTime hoje = LocalDateTime.now();
            
            // Requer o construtor de 5 argumentos adicionado na Appointment.java anteriormente
            appointmentRepository.save(new Appointment(house, lucas, hoje.minusDays(1), "Check-up", "COMPLETED"));
            appointmentRepository.save(new Appointment(house, lucas, hoje.withHour(14).withMinute(0), "Dor lombar", "SCHEDULED"));

            System.out.println("✅ Dados de demonstração carregados com sucesso!");
        } catch (Exception e) {
            System.err.println("⚠️ Erro na carga: " + e.getMessage());
        }
    }
}