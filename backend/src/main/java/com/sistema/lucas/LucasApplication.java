package com.sistema.lucas;

import com.sistema.lucas.config.WhatsAppProperties;
import com.sistema.lucas.domain.*;
import com.sistema.lucas.domain.enums.*;
import com.sistema.lucas.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(WhatsAppProperties.class)
public class LucasApplication {

    public static void main(String[] args) {
        SpringApplication.run(LucasApplication.class, args);
    }

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, 
                                       PatientRepository patientRepository, 
                                       DoctorRepository doctorRepository, 
                                       ExamRepository examRepository,
                                       AppointmentRepository appointmentRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                System.out.println("🧹 LIMPANDO DADOS ANTIGOS PARA CARGA DE TESTE...");
                // A ordem de deleção importa por causa das chaves estrangeiras (Foreign Keys)
                appointmentRepository.deleteAll();
                examRepository.deleteAll();
                doctorRepository.deleteAll();
                patientRepository.deleteAll();
                userRepository.deleteAll();

                System.out.println("🚀 INICIANDO CARGA DE DADOS FRESCOS...");

                // 1. ADMIN
                User admin = new User();
                admin.setName("Administrador");
                admin.setEmail("admin@clinica.com");
                admin.setPassword(passwordEncoder.encode("123456"));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
                System.out.println("✅ ADMIN CRIADO");

                // 2. MÉDICO
                Doctor doc = new Doctor();
                doc.setName("Dr. Gregory House");
                doc.setEmail("medico@teste.com");
                doc.setPassword(passwordEncoder.encode("123456"));
                doc.setRole(Role.DOCTOR);
                doc.setActive(true);
                doc.setCrm("12345-SP");
                doc.setSpecialty("Infectologia");
                doctorRepository.save(doc);
                System.out.println("✅ MÉDICO CRIADO");

                // 3. PACIENTE
                Patient pat = new Patient();
                pat.setName("Lucas Paciente");
                pat.setEmail("paciente@teste.com");
                pat.setPassword(passwordEncoder.encode("123456"));
                pat.setRole(Role.PATIENT);
                pat.setActive(true);
                pat.setCpf("111.222.333-44");
                pat.setWhatsapp("5511999998888");
                patientRepository.save(pat);
                System.out.println("✅ PACIENTE CRIADO");

                // 4. CONSULTA INICIAL (Para o médico ver a agenda cheia)
                Appointment app = new Appointment();
                app.setDoctor(doc);
                app.setPatient(pat);
                app.setStartTime(LocalDateTime.now().plusHours(2));
                app.setEndTime(LocalDateTime.now().plusHours(3));
                app.setStatus(AppointmentStatus.SCHEDULED);
                app.setReason("Consulta de teste inicial");
                appointmentRepository.save(app);
                System.out.println("📅 CONSULTA CRIADA");

            } catch (Exception e) {
                System.err.println("⚠️ Erro crítico na inicialização: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}