package com.sistema.lucas;

import com.sistema.lucas.config.WhatsAppProperties;
import com.sistema.lucas.domain.Exam;
import com.sistema.lucas.domain.Patient;
import com.sistema.lucas.domain.User;
import com.sistema.lucas.domain.enums.Role;
import com.sistema.lucas.repository.ExamRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.UserRepository;
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
                                       ExamRepository examRepository, // <-- Novo!
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            
            // ==========================================
            // 1. CRIAÇÃO DO ADMIN MASTER
            // ==========================================
            boolean adminExists = userRepository.findAll().stream()
                    .anyMatch(user -> user.getEmail().equals("admin@clinica.com"));

            if (!adminExists) {
                User admin = new User();
                admin.setName("Administrador da Clínica");
                admin.setEmail("admin@clinica.com");
                admin.setPassword(passwordEncoder.encode("123456"));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
                System.out.println("✅ CONTA ADMIN CRIADA COM SUCESSO!");
            }

            // ==========================================
            // 2. CRIAÇÃO DO PACIENTE DE TESTE (Retorna o Paciente Salvo)
            // ==========================================
            Patient patientDeTeste = (Patient) userRepository.findAll().stream()
                    .filter(user -> user.getEmail().equals("paciente@teste.com"))
                    .findFirst()
                    .orElseGet(() -> {
                        Patient p = new Patient();
                        p.setName("Lucas (Paciente de Teste)");
                        p.setEmail("paciente@teste.com");
                        p.setPassword(passwordEncoder.encode("123456")); 
                        p.setRole(Role.PATIENT);
                        p.setActive(true);
                        p.setCpf("111.222.333-44");
                        p.setWhatsapp("(11) 99999-8888");
                        
                        System.out.println("✅ CONTA PACIENTE CRIADA COM SUCESSO!");
                        return patientRepository.save(p);
                    });

            // ==========================================
            // 3. CRIAÇÃO DOS EXAMES FALSOS
            // ==========================================
            // Verifica se este paciente já tem exames para não duplicar toda a vez que reiniciar
            if (examRepository.findAllByPatientIdOrderByReleaseDateDesc(patientDeTeste.getId()).isEmpty()) {
                
                Exam exame1 = new Exam();
                exame1.setTitle("Hemograma Completo");
                exame1.setReleaseDate(LocalDateTime.now().minusDays(2));
                // Usamos um PDF genérico de teste da internet
                exame1.setFileUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"); 
                exame1.setPatient(patientDeTeste);

                Exam exame2 = new Exam();
                exame2.setTitle("Raio-X do Tórax");
                exame2.setReleaseDate(LocalDateTime.now().minusMonths(1));
                exame2.setFileUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
                exame2.setPatient(patientDeTeste);

                examRepository.save(exame1);
                examRepository.save(exame2);
                
                System.out.println("📄 EXAMES DE TESTE CRIADOS PARA O PACIENTE!");
            }
        };
    }
}