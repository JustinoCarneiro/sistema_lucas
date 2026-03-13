package com.sistema.lucas.config;

import com.sistema.lucas.model.*;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;

@Configuration
public class DataInitializer implements CommandLineRunner {

    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            
            // 1. Criar Médicos (Profissionais)
            Professional doc1 = new Professional();
            doc1.setName("Dr. Gregory House");
            doc1.setEmail("house@clinica.com");
            doc1.setPassword(passwordEncoder.encode("123456"));
            doc1.setCrm("CRM/SP 123456");
            doc1.setSpecialty("Infectologia e Diagnóstico");
            doc1.setRole(Role.PROFESSIONAL);
            professionalRepository.save(doc1);

            Professional doc2 = new Professional();
            doc2.setName("Dra. Meredith Grey");
            doc2.setEmail("grey@clinica.com");
            doc2.setPassword(passwordEncoder.encode("123456"));
            doc2.setCrm("CRM/RJ 654321");
            doc2.setSpecialty("Cirurgia Geral");
            doc2.setRole(Role.PROFESSIONAL);
            professionalRepository.save(doc2);

            // 2. Criar Pacientes
            Patient p1 = new Patient();
            p1.setName("Lucas Silva");
            p1.setEmail("lucas@email.com");
            p1.setPassword(passwordEncoder.encode("123456"));
            p1.setCpf("123.456.789-00");
            p1.setRole(Role.PATIENT);
            patientRepository.save(p1);

            Patient p2 = new Patient();
            p2.setName("Ana Oliveira");
            p2.setEmail("ana@email.com");
            p2.setPassword(passwordEncoder.encode("123456"));
            p2.setCpf("987.654.321-11");
            p2.setRole(Role.PATIENT);
            patientRepository.save(p2);

            // 3. Criar Consultas (Appointments)
            Appointment app1 = new Appointment();
            app1.setProfessional(doc1);
            app1.setPatient(p1);
            app1.setDateTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)); // Amanhã às 10h
            app1.setReason("Dor de cabeça persistente");
            app1.setStatus("SCHEDULED");
            appointmentRepository.save(app1);

            Appointment app2 = new Appointment();
            app2.setProfessional(doc2);
            app2.setPatient(p1);
            app2.setDateTime(LocalDateTime.now().minusDays(2).withHour(14).withMinute(30)); // Ontem às 14:30
            app2.setReason("Revisão pós-operatória");
            app2.setStatus("COMPLETED");
            appointmentRepository.save(app2);

            // 4. Criar um Admin para você
            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail("admin@clinica.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            System.out.println("✅ Dados fictícios carregados com sucesso!");
        }
    }
}