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

    public String exportPatientData(String email) {
        StringBuilder csv = new StringBuilder();
        csv.append("TIPO;TITULO;DATA;NOTAS/CONTEUDO\n");
        
        // Prontuários do paciente
        List<Prontuario> prontuarios = prontuarioRepository.findAll().stream()
                .filter(p -> p.getPatient().getEmail().equals(email))
                .collect(Collectors.toList());
        for (Prontuario p : prontuarios) {
            String notasSeguras = p.getNotas() != null ? p.getNotas().replace("\"", "'").replace("\n", " ") : "";
            csv.append(String.format("PRONTUARIO;Atendimento;%s;\"%s\"\n",
                    p.getCriadoEm(), notasSeguras));
        }

        // Documentos disponíveis para o paciente
        List<Documento> docs = documentoRepository.findByPacienteEmailAndDisponivelTrueOrderByCriadoEmDesc(email);
        for (Documento d : docs) {
            String conteudoSeguro = d.getConteudoTexto() != null ? d.getConteudoTexto().replace("\"", "'").replace("\n", " ") : "PDF";
            csv.append(String.format("DOCUMENTO;%s;%s;\"%s\"\n",
                    d.getTitulo(), d.getCriadoEm(), conteudoSeguro));
        }

        return csv.toString();
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
