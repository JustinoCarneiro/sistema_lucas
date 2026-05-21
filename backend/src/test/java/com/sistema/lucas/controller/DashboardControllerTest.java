package com.sistema.lucas.controller;

import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.*;
import com.sistema.lucas.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@SuppressWarnings("null")
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext context;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AppointmentRepository appointmentRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private ProfessionalRepository professionalRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private PatientRepository patientRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private ProntuarioRepository prontuarioRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private DocumentoRepository documentoRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AuditLogService auditLogService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(Objects.requireNonNull(context))
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /dashboard/admin — ADMIN recebe 200 com métricas")
    void dashboardAdmin_admin_retorna200() throws Exception {
        when(appointmentRepository.countByDateRange(any(), any())).thenReturn(List.of());
        when(appointmentRepository.countByStatus(any(StatusConsulta.class))).thenReturn(0L);

        mockMvc.perform(get("/dashboard/admin")
                .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProfissionais").exists())
                .andExpect(jsonPath("$.totalPacientes").exists())
                .andExpect(jsonPath("$.consultasHoje").exists());
    }

    @Test
    @DisplayName("GET /dashboard/profissional — PROFESSIONAL recebe 200 com campo consultasAtrasadas")
    void dashboardProfissional_professional_retorna200ComAtrasadas() throws Exception {
        when(appointmentRepository.findTodayAppointmentsByProfessionalEmail(anyString())).thenReturn(List.of());
        when(appointmentRepository.countAtrasadasByProfessionalEmail(anyString(), any(), any())).thenReturn(2L);
        when(appointmentRepository.findProximasByProfissionalEmail(anyString(), any())).thenReturn(List.of());
        when(prontuarioRepository.findByProfessionalEmailOrderByCriadoEmDesc(anyString())).thenReturn(List.of());
        when(documentoRepository.findByProfissionalEmailOrderByCriadoEmDesc(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/profissional")
                .with(user("prof@test.com").roles("PROFESSIONAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultasAtrasadas").value(2));
    }

    @Test
    @DisplayName("GET /dashboard/paciente — PATIENT recebe 200 com pendentesConfirmacao")
    void dashboardPaciente_patient_retorna200() throws Exception {
        when(appointmentRepository.findProximaByPacienteEmail(anyString(), any())).thenReturn(List.of());
        when(documentoRepository.findByPacienteEmailAndDisponivelTrueOrderByCriadoEmDesc(anyString())).thenReturn(List.of());
        when(patientRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard/paciente")
                .with(user("pac@test.com").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendentesConfirmacao").exists());
    }

    @Test
    @DisplayName("GET /dashboard/profissional — sem autenticação retorna 4xx")
    void dashboardProfissional_semAuth_retorna4xx() throws Exception {
        mockMvc.perform(get("/dashboard/profissional"))
                .andExpect(status().is4xxClientError());
    }
}
