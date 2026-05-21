package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Documento;
import com.sistema.lucas.model.Prontuario;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.DocumentoRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProntuarioRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final AppointmentRepository appointmentRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final DocumentoRepository documentoRepository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;

    public String exportAdminData() {
        StringBuilder csv = new StringBuilder();
        csv.append("ID;Paciente;Profissional;Data;Status\n");
        List<Appointment> all = appointmentRepository.findAll();
        for (Appointment a : all) {
            csv.append(String.format("%d;%s;%s;%s;%s\n",
                    a.getId(), 
                    a.getPatient() != null ? a.getPatient().getName() : "N/A", 
                    a.getProfessional() != null ? a.getProfessional().getName() : "N/A", 
                    a.getDateTime(), 
                    a.getStatus()));
        }
        return csv.toString();
    }

    public String exportProfessionalData(String email) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID;Data;Paciente;Notas\n");
        List<Prontuario> logs = prontuarioRepository.findAll().stream()
                .filter(p -> p.getProfessional().getEmail().equals(email))
                .collect(Collectors.toList());
        for (Prontuario p : logs) {
            String notasSeguras = p.getNotas() != null ? p.getNotas().replace("\"", "'").replace("\n", " ") : "";
            csv.append(String.format("%d;%s;%s;\"%s\"\n",
                    p.getId(), p.getCriadoEm(), p.getPatient().getName(), notasSeguras));
        }
        return csv.toString();
    }

    /**
     * AUD-06 (LGPD Art. 18, V — Portabilidade):
     * Exportação completa dos dados do titular em formato JSON estruturado,
     * incluindo dados cadastrais, prontários, documentos e consultas.
     */
    public String exportPatientData(String email) {
        Patient patient = patientRepository.findByEmail(email).orElse(null);
        if (patient == null) return "{\"erro\": \"Paciente não encontrado\"}";

        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // 1. Dados cadastrais
        json.append("  \"dados_cadastrais\": {\n");
        json.append("    \"nome\": \"").append(esc(patient.getName())).append("\",\n");
        json.append("    \"email\": \"").append(esc(patient.getEmail())).append("\",\n");
        json.append("    \"cpf\": \"").append(maskCpf(patient.getCpf())).append("\",\n");
        json.append("    \"telefone\": \"").append(esc(patient.getPhone())).append("\",\n");
        json.append("    \"endereco\": \"").append(esc(patient.getAddress())).append("\",\n");
        json.append("    \"data_nascimento\": \"").append(patient.getBirthDate() != null ? patient.getBirthDate().toString() : "").append("\",\n");
        json.append("    \"genero\": \"").append(esc(patient.getGender())).append("\",\n");
        json.append("    \"contato_emergencia_nome\": \"").append(esc(patient.getEmergencyContactName())).append("\",\n");
        json.append("    \"contato_emergencia_telefone\": \"").append(esc(patient.getEmergencyContactPhone())).append("\",\n");
        json.append("    \"alergias\": \"").append(esc(patient.getAllergies())).append("\"\n");
        json.append("  },\n");

        // 2. Prontários
        List<Prontuario> prontuarios = prontuarioRepository.findAll().stream()
                .filter(p -> p.getPatient().getEmail().equals(email))
                .collect(Collectors.toList());
        json.append("  \"prontuarios\": [\n");
        for (int i = 0; i < prontuarios.size(); i++) {
            Prontuario p = prontuarios.get(i);
            json.append("    {\"data\": \"").append(p.getCriadoEm()).append("\", ");
            json.append("\"profissional\": \"").append(esc(p.getProfessional().getName())).append("\", ");
            json.append("\"notas\": \"").append(esc(p.getNotas())).append("\"}");
            json.append(i < prontuarios.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n");

        // 3. Documentos
        List<Documento> docs = documentoRepository.findByPacienteEmailAndDisponivelTrueOrderByCriadoEmDesc(email);
        json.append("  \"documentos\": [\n");
        for (int i = 0; i < docs.size(); i++) {
            Documento d = docs.get(i);
            json.append("    {\"titulo\": \"").append(esc(d.getTitulo())).append("\", ");
            json.append("\"tipo\": \"").append(esc(d.getTipo() != null ? d.getTipo().name() : "")).append("\", ");
            json.append("\"data\": \"").append(d.getCriadoEm()).append("\"}");
            json.append(i < docs.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n");

        // 4. Consultas
        List<Appointment> consultas = appointmentRepository.findByPatientId(patient.getId());
        json.append("  \"consultas\": [\n");
        for (int i = 0; i < consultas.size(); i++) {
            Appointment a = consultas.get(i);
            json.append("    {\"data\": \"").append(a.getDateTime()).append("\", ");
            json.append("\"profissional\": \"").append(esc(a.getProfessional() != null ? a.getProfessional().getName() : "")).append("\", ");
            json.append("\"status\": \"").append(a.getStatus()).append("\"}");
            json.append(i < consultas.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n");

        // 5. Metadados LGPD
        json.append("  \"lgpd\": {\n");
        json.append("    \"consentimento_aceito\": ").append(patient.isTermsAccepted()).append(",\n");
        json.append("    \"data_consentimento\": \"").append(patient.getTermsAcceptedAt() != null ? patient.getTermsAcceptedAt().toString() : "").append("\",\n");
        json.append("    \"versao_termos\": \"").append(esc(patient.getTermsVersion())).append("\",\n");
        json.append("    \"data_exportacao\": \"").append(java.time.LocalDateTime.now()).append("\"\n");
        json.append("  }\n");

        json.append("}");
        return json.toString();
    }

    private String esc(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    public String exportPatientsData() {
        StringBuilder csv = new StringBuilder();
        csv.append("ID;Nome;Email;Telefone;CPF;Data Nasc\n");
        List<Patient> all = patientRepository.findAll();
        for (Patient p : all) {
            String maskedCpf = maskCpf(p.getCpf());
            csv.append(String.format("%d;%s;%s;%s;%s;%s\n",
                    p.getId(), p.getName(), p.getEmail(), p.getPhone(), maskedCpf, p.getBirthDate()));
        }
        return csv.toString();
    }

    public String exportProfessionalsData() {
        StringBuilder csv = new StringBuilder();
        csv.append("ID;Nome;Registro;Especialidade;Email;Telefone\n");
        List<Professional> all = professionalRepository.findAll();
        for (Professional p : all) {
            csv.append(String.format("%d;%s;%s;%s;%s;%s\n",
                    p.getId(), p.getName(), p.getRegistroConselho(), p.getSpecialty(), p.getEmail(), p.getPhone()));
        }
        return csv.toString();
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) return "N/A";
        // Formato esperado: 123.456.789-00 ou 12345678900
        String clean = cpf.replaceAll("[^0-9]", "");
        if (clean.length() != 11) return cpf;
        return clean.substring(0, 3) + ".***.***-" + clean.substring(9, 11);
    }
}
