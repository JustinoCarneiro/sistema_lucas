// backend/src/main/java/com/sistema/lucas/config/DataInitializer.java
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
            System.out.println("🧹 LIMPANDO BANCO PARA SINCRONIZAÇÃO DE CREDENCIAIS...");
            appointmentRepository.deleteAll();
            professionalRepository.deleteAll();
            patientRepository.deleteAll();
            userRepository.deleteAll();

            // 1. Criar ADMIN
            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail("admin@clinica.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            // 2. Criar Médico (Gregory House)
            Professional doc = new Professional();
            doc.setName("Dr. Gregory House");
            doc.setEmail("house@clinica.com");
            doc.setPassword(passwordEncoder.encode("123456"));
            doc.setCrm("12345-SP");
            doc.setSpecialty("Infectologia");
            doc.setRole(Role.PROFESSIONAL);
            professionalRepository.save(doc);

            // 3. Criar Paciente (Lucas Silva) - O SEU ACESSO
            Patient lucas = new Patient();
            lucas.setName("Lucas Silva");
            lucas.setEmail("lucas@email.com");
            lucas.setPassword(passwordEncoder.encode("123456"));
            lucas.setCpf("111.222.333-44");
            lucas.setRole(Role.PATIENT);
            patientRepository.save(lucas);

            // 4. Criar Consulta de Exemplo
            Appointment app = new Appointment();
            app.setProfessional(doc);
            app.setPatient(lucas);
            app.setDateTime(LocalDateTime.now().plusDays(1));
            app.setReason("Check-up Geral");
            app.setStatus("SCHEDULED");
            appointmentRepository.save(app);

            System.out.println("✅ CARGA ÚNICA CONCLUÍDA COM SUCESSO!");
            System.out.println("👉 LOGIN PACIENTE: lucas@email.com | 123456");

        } catch (Exception e) {
            System.err.println("⚠️ Erro na carga: " + e.getMessage());
        }
    }
}