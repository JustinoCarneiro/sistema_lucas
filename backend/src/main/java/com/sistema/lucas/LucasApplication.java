package com.sistema.lucas;

import com.sistema.lucas.model.*;
import com.sistema.lucas.model.enums.*;
import com.sistema.lucas.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

@SpringBootApplication
public class LucasApplication {

    public static void main(String[] args) {
        SpringApplication.run(LucasApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, 
                                     PatientRepository patientRepository, 
                                     ProfessionalRepository professionalRepository, 
                                     AppointmentRepository appointmentRepository,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                System.out.println("🧹 LIMPANDO BANCO PARA NOVO PADRÃO MVC...");
                appointmentRepository.deleteAll();
                professionalRepository.deleteAll();
                patientRepository.deleteAll();
                userRepository.deleteAll();

                // 1. Criar Usuário ADMIN (para segurança)
                User admin = new User("admin@clinica.com", passwordEncoder.encode("admin123"), Role.ADMIN);
                userRepository.save(admin);

                // 2. Criar Profissional (usando o novo construtor via DTO se preferir, ou manual)
                Professional doc = new Professional();
                doc.setName("Dr. Gregory House");
                doc.setEmail("house@teste.com");
                doc.setPassword(passwordEncoder.encode("123456"));
                doc.setCrm("12345-SP");
                doc.setSpecialty("Infectologia");
                professionalRepository.save(doc);

                // 3. Criar Paciente
                Patient pat = new Patient();
                pat.setName("Lucas Silva");
                pat.setEmail("paciente@teste.com");
                pat.setPassword(passwordEncoder.encode("123456"));
                pat.setCpf("111.222.333-44");
                patientRepository.save(pat);

                // 4. Criar Consulta
                Appointment app = new Appointment();
                app.setProfessional(doc);
                app.setPatient(pat);
                app.setDateTime(LocalDateTime.now().plusDays(1));
                app.setReason("Check-up Geral");
                app.setStatus("SCHEDULED");
                appointmentRepository.save(app);

                System.out.println("✅ CARGA INICIAL CONCLUÍDA COM SUCESSO!");

            } catch (Exception e) {
                System.err.println("⚠️ Erro na carga: " + e.getMessage());
            }
        };
    }
}